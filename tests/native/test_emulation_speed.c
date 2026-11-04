#include "native/app/emulation_speed.h"
#include <assert.h>
#include <stdio.h>

static unsigned checks;

static void status(NcSpeedReading reading, int expected)
{
    assert(reading.status == expected);
    if (expected != NC_SPEED_READY) assert(reading.percent == -1.0f);
    checks++;
}

static void percent(NcSpeedReading reading, float expected)
{
    status(reading, NC_SPEED_READY);
    const float difference = reading.percent - expected;
    assert(difference >= -0.001f && difference <= 0.001f);
    checks++;
}

int main(void)
{
    NcSpeedMeter meter = {0};
    status(nc_speed_sample(&meter, 0, 0, 0, false, false), NC_SPEED_UNAVAILABLE);
    status(nc_speed_sample(&meter, 0, 0, 0, false, true), NC_SPEED_UNAVAILABLE);
    status(nc_speed_sample(&meter, 1, 1, 1, true, false), NC_SPEED_WARMING);
    status(nc_speed_sample(&meter, 500000001ull, 500000001ull, 1, true, false), NC_SPEED_WARMING);
    percent(nc_speed_sample(&meter, 1000000001ull, 1000000001ull, 1, true, false), 100.0f);
    percent(nc_speed_sample(&meter, 2000000001ull, 1500000001ull, 1, true, false), 50.0f);
    percent(nc_speed_sample(&meter, 3000000001ull, 1500000001ull, 1, true, false), 0.0f);
    /* Long stalls stay in the sample rather than being discarded as gaps. */
    percent(nc_speed_sample(&meter, 13000000001ull, 2500000001ull, 1, true, false), 10.0f);
    status(nc_speed_sample(&meter, 14000000001ull, 2500000001ull, 2, true, true), NC_SPEED_PAUSED);
    status(nc_speed_sample(&meter, 30000000001ull, 2500000001ull, 3, true, false), NC_SPEED_WARMING);
    percent(nc_speed_sample(&meter, 31000000001ull, 3500000001ull, 3, true, false), 100.0f);
    /* A pause and resume entirely between polls must not look like slowdown. */
    status(nc_speed_sample(&meter, 40000000001ull, 4000000001ull, 5, true, false), NC_SPEED_WARMING);
    percent(nc_speed_sample(&meter, 41000000001ull, 5000000001ull, 5, true, false), 100.0f);
    /* Restoring forwards or backwards starts a new window. */
    status(nc_speed_sample(&meter, 42000000001ull, 100000000001ull, 6, true, false), NC_SPEED_WARMING);
    percent(nc_speed_sample(&meter, 43000000001ull, 100500000001ull, 6, true, false), 50.0f);
    status(nc_speed_sample(&meter, 44000000001ull, 1, 7, true, false), NC_SPEED_WARMING);
    percent(nc_speed_sample(&meter, 45000000001ull, 1000000001ull, 7, true, false), 100.0f);
    /* Defensive reset for an unexpected backwards host or guest timestamp. */
    status(nc_speed_sample(&meter, 45000000000ull, 1000000001ull, 7, true, false), NC_SPEED_WARMING);
    status(nc_speed_sample(&meter, 45000000001ull, 1000000000ull, 7, true, false), NC_SPEED_WARMING);
    status(nc_speed_sample(&meter, 45000000002ull, 1000000000ull, 7, false, false), NC_SPEED_UNAVAILABLE);
    /* Many ratios and large monotonic origins: no dependency on game FPS. */
    for (unsigned ratio = 0; ratio <= 200; ratio++) {
        for (unsigned step = 1; step <= 9; step++) {
            meter = (NcSpeedMeter){0};
            const uint64_t origin = 987654321000000ull;
            const uint64_t duration = (uint64_t)step * 1000000000ull;
            status(nc_speed_sample(&meter, origin, origin, 0, true, false), NC_SPEED_WARMING);
            percent(nc_speed_sample(&meter, origin + duration, origin + duration * ratio / 100,
                                    0, true, false), (float)ratio);
        }
    }
    printf("PASS: %u virtual-clock speed checks\n", checks);
    return 0;
}
