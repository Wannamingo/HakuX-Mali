/*
 * hakuX native core, app front end — presenting the HLE GPU's front image.
 *
 * Once per display refresh the window's thread hands its Android window to
 * the HLE GPU's presenter (hw/xbox/hle/vk/swapchain.h), which draws the
 * front frame into it through a Vulkan swapchain: here, the frame's place in
 * the window (letterboxed to the chosen shape) and the anti-aliasing chosen
 * (nc_present_set_fxaa, nc_present_set_upscale).
 */
#include "native/app/app.h"
#include "native/dev/dev.h"
#include "hw/xbox/hle/vk/display.h"
#include "hw/xbox/hle/vk/swapchain.h"
#include "hw/xbox/hle/vk/back.h"
#include "native/core/movie.h"

#include <string.h>
#include <sys/system_properties.h>
#include "hw/xbox/hle/prop.h"
#include <time.h>
#include <pthread.h>

static int g_aspect = NC_ASPECT_AUTO;
static bool g_widescreen;
static bool g_fxaa;
static int g_upscale = NC_UPSCALE_BILINEAR;
static bool g_shown;
static int g_movie_mode;
static bool g_movie_force;
static uint32_t g_movie_w,g_movie_h;
void nc_present_set_movie(int mode)
{
    mode=mode>=0&&mode<=3?mode:0;
    int old=__atomic_exchange_n(&g_movie_mode,mode,__ATOMIC_ACQ_REL);
    if(old!=mode)__atomic_store_n(&g_movie_force,false,__ATOMIC_RELEASE);
    hle_back_movie_tracking(mode!=0);nc_movie_enable(hle_back_movie_tracking_on());
}
bool nc_present_toggle_movie(void)
{
    bool force=!__atomic_load_n(&g_movie_force,__ATOMIC_ACQUIRE);
    __atomic_store_n(&g_movie_force,force,__ATOMIC_RELEASE);return force;
}
static void movie_layout(void)
{
    uint64_t source=hle_back_movie_status();
    int mode=__atomic_load_n(&g_movie_mode,__ATOMIC_ACQUIRE);
    bool force=__atomic_load_n(&g_movie_force,__ATOMIC_ACQUIRE);
    g_movie_w=g_movie_h=0;
    if(force||(mode&&source)) {
        if(mode==2) {g_movie_w=16;g_movie_h=9;}
        else if(mode==3&&source) {g_movie_w=source&0xffffu;g_movie_h=source>>16&0xffffu;}
        else {g_movie_w=4;g_movie_h=3;}
    }
}
static pthread_mutex_t g_framing_lock = PTHREAD_MUTEX_INITIALIZER;
static int g_pending_framing[7] = {0, 0, 0, 0, 100, 0, 0};
static int g_framing[7] = {0, 0, 0, 0, 100, 0, 0};
static unsigned g_framing_generation, g_applied_framing;

void nc_present_set_framing(int left, int right, int top, int bottom, int zoom, int shift_x, int shift_y)
{
    int v[7] = {left, right, top, bottom, zoom, shift_x, shift_y};
    for (int i = 0; i < 4; i++) v[i] = v[i] < 0 ? 0 : v[i] > 15 ? 15 : v[i];
    v[4] = zoom < 100 ? 100 : zoom > 200 ? 200 : zoom;
    for (int i = 5; i < 7; i++) v[i] = v[i] < -20 ? -20 : v[i] > 20 ? 20 : v[i];
    pthread_mutex_lock(&g_framing_lock);
    if (memcmp(g_pending_framing, v, sizeof(v))) {
        memcpy(g_pending_framing, v, sizeof(v));
        __atomic_add_fetch(&g_framing_generation, 1, __ATOMIC_RELEASE);
    }
    pthread_mutex_unlock(&g_framing_lock);
}

static void apply_framing(void)
{
    if (__atomic_load_n(&g_framing_generation, __ATOMIC_ACQUIRE) == g_applied_framing) return;
    pthread_mutex_lock(&g_framing_lock);
    memcpy(g_framing, g_pending_framing, sizeof(g_framing));
    g_applied_framing = __atomic_load_n(&g_framing_generation, __ATOMIC_RELAXED);
    pthread_mutex_unlock(&g_framing_lock);
    hle_present_set_framing(g_framing[0], g_framing[1], g_framing[2], g_framing[3], g_framing[4], g_framing[5], g_framing[6]);
    nc_log("hakuX-app", "framing: crop %d,%d,%d,%d%% zoom %d%% shift %d,%d%%", g_framing[0], g_framing[1],
           g_framing[2], g_framing[3], g_framing[4], g_framing[5], g_framing[6]);
}

/* Frame rate from the HLE GPU's presented frames. */
static uint64_t g_win_presented, g_win_start_ns, g_last_present_ns, g_seen_presented;
static float g_fps, g_avg_ms, g_last_ms;

/* debug.hakux.present, for comparisons on the device (read every 64
 * refreshes): "fxaa" / "nofxaa", "sgsr" / "bilinear" (the upscaling),
 * "noaverage" (a larger frame sampled bilinearly); otherwise the settings. */
static struct {
    int fxaa, upscale;      /* -1: the setting's */
    bool average;
} g_dbg = {-1, -1, true};

bool nc_present_init(void)
{
    hle_disp_gamma_by_consumer(true); /* before the core starts: frames come without it */
    nc_log("hakuX-app", "present: Vulkan; FXAA %s, upscaling %s", g_fxaa ? "on" : "off",
           g_upscale == NC_UPSCALE_SGSR ? "SGSR" : "bilinear");
    return true;
}

void nc_present_set_shader(int mode)
{ hle_present_set_shader(mode); }
void nc_present_set_shader_controls(const int values[8])
{ hle_present_set_shader_controls(values); }

void nc_present_set_fxaa(bool on)
{
    g_fxaa = on;
}

void nc_present_set_upscale(int mode)
{
    g_upscale = mode;
}

void nc_present_set_picture(int brightness, int contrast, int saturation, int gamma, int sharpness)
{
    hle_present_set_picture(brightness, contrast, saturation, gamma, sharpness);
    nc_log("hakuX-app", "picture: brightness %d%%, contrast %d%%, saturation %d%%, gamma %d%%, SGSR %d%%",
           brightness, contrast, saturation, gamma, sharpness);
}

void nc_present_set_aspect(int mode)
{
    g_aspect = mode;
}

void nc_present_set_widescreen(bool on)
{
    g_widescreen = on;
}

static uint64_t now_ns(void)
{
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (uint64_t)ts.tv_sec * 1000000000ull + (uint64_t)ts.tv_nsec;
}

static void count_frames(void)
{
    const uint64_t now = now_ns();
    const uint64_t presented = hle_disp_stats().presented;
    if (!g_win_start_ns) {
        g_win_start_ns = now;
        g_win_presented = g_seen_presented = presented;
        return;
    }
    if (presented > g_seen_presented) {
        if (g_last_present_ns) {
            g_last_ms = (float)(now - g_last_present_ns) / 1e6f / (float)(presented - g_seen_presented);
        }
        g_last_present_ns = now;
        g_seen_presented = presented;
    }
    if (now - g_win_start_ns >= 500000000ull) {
        const uint64_t n = presented - g_win_presented;
        const float ms = (float)(now - g_win_start_ns) / 1e6f;
        g_fps = (float)n * 1000.0f / ms;
        g_avg_ms = n ? ms / (float)n : 0.0f;
        g_win_start_ns = now;
        g_win_presented = presented;
    }
}

void nc_present_stats(float *fps, float *avg_ms, float *last_ms)
{
    *fps = g_fps;
    *avg_ms = g_avg_ms;
    *last_ms = g_last_ms;
}

bool nc_present_shown(void)
{
    return __atomic_load_n(&g_shown, __ATOMIC_RELAXED);
}

static void debug_poll(void)
{
    static unsigned n;
    if (n++ % 64) {
        return;
    }
    char v[PROP_VALUE_MAX] = "";
    hle_prop_get("debug.hakux.present", v);
    g_dbg.fxaa = strstr(v, "nofxaa") ? 0 : strstr(v, "fxaa") ? 1 : -1;
    g_dbg.upscale = strstr(v, "sgsr") ? NC_UPSCALE_SGSR : strstr(v, "bilinear") ? NC_UPSCALE_BILINEAR : -1;
    g_dbg.average = !strstr(v, "noaverage");
}

/* The frame's half size in the window (clip space): letterboxed to the
 * chosen shape, the frame's own (4:3 for 640x480), 4:3, 16:9, or the
 * window's (stretched). */
static void place(uint32_t fw, uint32_t fh, uint32_t ww, uint32_t wh, float *sx, float *sy)
{
    const float wa = (float)ww / (float)wh;
    float fa = (float)fw / (float)fh;
    switch (g_aspect) {
    case NC_ASPECT_4_3:
        fa = 4.0f / 3.0f;
        break;
    case NC_ASPECT_16_9:
        fa = 16.0f / 9.0f;
        break;
    case NC_ASPECT_STRETCH:
        fa = wa;
        break;
    default:
        /* A standard-definition frame (640x480, 720x480: under 16:10) from a
         * console set to widescreen is anamorphic 16:9. */
        if (g_widescreen && fa < 1.6f) {
            fa = 16.0f / 9.0f;
        }
        break;
    }
    if(g_movie_w&&g_movie_h)fa=(float)g_movie_w/g_movie_h;
    fa *= (100.0f - g_framing[0] - g_framing[1]) / (100.0f - g_framing[2] - g_framing[3]);
    *sx = *sy = 1.0f;
    if (wa > fa) {
        *sx = fa / wa;
    } else {
        *sy = wa / fa;
    }
    /* One line when the source/window/setting changes: verifies the actual
     * rectangle handed to the Vulkan presenter, without a per-frame trace. */
    static uint64_t last_frame, last_window;
    static int last_mode = -1;
    const uint64_t frame = (uint64_t)fw << 32 | fh, window = (uint64_t)ww << 32 | wh;
    const int mode = g_aspect * 2 + g_widescreen + (g_movie_w ? (g_movie_w*4096+g_movie_h)*16:0);
    if (frame != last_frame || window != last_window || mode != last_mode) {
        const char *aspect = g_aspect == NC_ASPECT_4_3 ? "4:3" :
                             g_aspect == NC_ASPECT_16_9 ? "16:9" :
                             g_aspect == NC_ASPECT_STRETCH ? "stretch" : "auto";
        const uint32_t vw = (uint32_t)(ww * *sx + 0.5f), vh = (uint32_t)(wh * *sy + 0.5f);
        nc_log("hakuX-app", "present layout: aspect=%s frame=%ux%u window=%ux%u viewport=%ux%u offset=%u,%u",
               aspect, fw, fh, ww, wh, vw, vh, (ww - vw) / 2, (wh - vh) / 2);
        last_frame = frame;
        last_window = window;
        last_mode = mode;
        if(g_movie_w)nc_log("hakuX-app","movie layout: %u:%u",g_movie_w,g_movie_h);
    }
}

/* With nothing new to show, the refresh goes by: the panel keeps the image,
 * and the loop waits for the next vsync, as acquiring an image paced it. */
static void wait_next_vsync(void)
{
    const uint64_t now = now_ns(), next = nc_dev_nv2a_next_vsync(now);
    const uint64_t until = next ? next + 500000ull : now + 4000000ull;
    const struct timespec ts = {(time_t)(until / 1000000000ull), (long)(until % 1000000000ull)};
    clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &ts, NULL);
}

bool nc_present_frame(struct ANativeWindow *win)
{
    hle_present_prepare_shader();
    apply_framing();
    movie_layout();
    count_frames();
    debug_poll();
    const bool fxaa = g_dbg.fxaa < 0 ? g_fxaa : g_dbg.fxaa != 0;
    const int upscale = g_dbg.upscale < 0 ? g_upscale : g_dbg.upscale;
    const int shader = hle_present_shader_mode();
    /* what places and filters the frame, beside it and the window */
    const uint64_t key = ((uint64_t)hle_present_shader_generation()*0x9e3779b97f4a7c15ull) ^ ((uint64_t)fxaa | (uint64_t)upscale << 1 | (uint64_t)g_dbg.average << 4 |
                         (uint64_t)g_widescreen << 5 | (uint64_t)(uint32_t)g_aspect << 8 | (uint64_t)shader << 12 |
                         (uint64_t)g_movie_w<<24 | (uint64_t)g_movie_h<<40);
    if (shader != 4 && hle_present_unchanged(win, key)) {
        nc_dev_nv2a_display_pickup(now_ns()); /* it looked: the vblank keeps clear of it */
        wait_next_vsync();
        return true;
    }
    uint32_t ww = 0, wh = 0, fw = 0, fh = 0;
    if (!hle_present_begin(win, &ww, &wh)) {
        return false;
    }
    nc_dev_nv2a_display_pickup(now_ns()); /* the vblank keeps clear of it */
    float sx = 1.0f, sy = 1.0f;
    if (hle_present_frame(&fw, &fh)) {
        place(fw, fh, ww, wh, &sx, &sy);
        __atomic_store_n(&g_shown, true, __ATOMIC_RELAXED);
    }
    hle_present_end(sx, sy, fxaa, upscale, g_dbg.average, key);
    return true;
}
