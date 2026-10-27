/*
 * File-backed Xbox images have the game partition, but need not include the
 * signed security descriptor from the physical DVD's lead-in. XAPI titles
 * whose certificate SecurityFlags bit 0 is set read that descriptor before
 * starting (Syberia, Rayman 3, The Sands of Time, Ultimate Beach Soccer).
 *
 * Tell XAPI about the emulated media at load time, in the in-memory copy of
 * the certificate. Keep the image on disk, all other certificate fields and
 * the kernel's SHA/RSA signature verification unchanged. The same runtime
 * certificate policy is used by Cxbx-Reloaded's ApplyMediaPatches:
 * https://github.com/Cxbx-Reloaded/Cxbx-Reloaded/blob/585c49a50af1255ab155099e06f24505f9c5a800/src/core/kernel/init/KrnlPatches.hpp
 */
#include "native/core/xbe_media.h"

static uint32_t rd32(const uint8_t *p)
{
    return (uint32_t)p[0] | (uint32_t)p[1] << 8 | (uint32_t)p[2] << 16 | (uint32_t)p[3] << 24;
}

bool nc_xbe_file_media(uint8_t *hdr, uint32_t hdr_len)
{
    const uint32_t security_off = 0x1D8;
    if (!hdr || hdr_len < 0x11C || rd32(hdr) != 0x48454258u) {
        return false;
    }
    const uint32_t base = rd32(hdr + 0x104), cert = rd32(hdr + 0x118);
    if (cert < base || cert - base > hdr_len || hdr_len - (cert - base) < 4) {
        return false;
    }
    uint8_t *p = hdr + (cert - base);
    const uint32_t size = rd32(p);
    if (size < security_off + 4 || size > hdr_len - (cert - base) || !(p[security_off] & 1)) {
        return false;
    }
    p[security_off] &= 0xFE;
    return true;
}
