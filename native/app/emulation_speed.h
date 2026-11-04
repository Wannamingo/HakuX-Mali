/* Read-only HUD measurement of virtual Xbox clock progress, independent of FPS. */
#ifndef NATIVE_APP_EMULATION_SPEED_H
#define NATIVE_APP_EMULATION_SPEED_H

#include <stdbool.h>
#include <stdint.h>

enum {
    NC_SPEED_UNAVAILABLE = 0,
    NC_SPEED_WARMING = 1,
    NC_SPEED_READY = 2,
    NC_SPEED_PAUSED = 3,
};

typedef struct NcSpeedMeter {
    uint64_t start_host, start_guest, last_host, last_guest;
    uint32_t epoch;
    float percent;
    bool initialized, measured;
} NcSpeedMeter;

typedef struct NcSpeedReading {
    float percent;
    int status;
} NcSpeedReading;

/* Only the HUD sampler calls this, not the guest's execution path. A pause
 * or state restore changes epoch, including pauses entirely between polls.
 * A long GPU stall is deliberately included, rather than hidden as a gap. */
static inline NcSpeedReading nc_speed_sample(NcSpeedMeter *meter, uint64_t host, uint64_t guest,
                                             uint32_t epoch, bool running, bool paused)
{
    if (!running || paused) {
        *meter = (NcSpeedMeter){0};
        return (NcSpeedReading){-1.0f, running ? NC_SPEED_PAUSED : NC_SPEED_UNAVAILABLE};
    }
    if (!meter->initialized || epoch != meter->epoch || host <= meter->last_host ||
        guest < meter->last_guest) {
        *meter = (NcSpeedMeter){.start_host = host, .start_guest = guest, .last_host = host,
                               .last_guest = guest, .epoch = epoch, .initialized = true};
        return (NcSpeedReading){-1.0f, NC_SPEED_WARMING};
    }
    meter->last_host = host;
    meter->last_guest = guest;
    const uint64_t elapsed = host - meter->start_host;
    if (elapsed >= 1000000000ull) {
        meter->percent = (float)(100.0 * (double)(guest - meter->start_guest) / (double)elapsed);
        meter->measured = true;
        meter->start_host = host;
        meter->start_guest = guest;
    }
    return meter->measured ? (NcSpeedReading){meter->percent, NC_SPEED_READY}
                           : (NcSpeedReading){-1.0f, NC_SPEED_WARMING};
}

#endif
