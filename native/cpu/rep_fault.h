/* Restart state for FEX's non-atomic REP MOVS loop. Kept outside FEX so the
 * embedding can deliver precise guest exceptions without changing the JIT. */
#pragma once
#include <stddef.h>
#include <stdint.h>
#include <stdbool.h>

static inline bool nc_rep_movs_progress(const uint8_t *code, size_t available,
                                       uint32_t insn, uint64_t membase,
                                       const uint64_t *host, uint32_t flags,
                                       uint32_t *src, uint32_t *dst, uint32_t *count)
{
    bool rep = false, word = false;
    size_t i = 0;
    for (; i < available; ++i) {
        if (code[i] == 0xf3 || code[i] == 0xf2) rep = true;
        else if (code[i] == 0x66) word = true;
        else break;
    }
    if (!rep || i == available || (code[i] != 0xa4 && code[i] != 0xa5) || !*count)
        return false;
    const unsigned unit = code[i] == 0xa4 ? 1 : word ? 2 : 4;
    const int direction = flags & 0x400 ? -1 : 1;
    unsigned bytes;
    bool load;
    if ((insn & 0xff800000u) == 0xac800000u) { /* LDP/STP Q, post-index */
        bytes = 32;
        load = (insn >> 22) & 1;
        const int offset = (int)((insn >> 15) & 127) - ((insn & (1u << 21)) ? 128 : 0);
        if ((insn & 31) != 0 || ((insn >> 10) & 31) != 1 || offset * 16 != direction * 32)
            return false;
    } else if ((insn & 0x3f200c00u) == 0x38000400u) { /* LDR/STR, post-index */
        bytes = 1u << (insn >> 30);
        const unsigned opc = (insn >> 22) & 3;
        load = opc == 1;
        const int offset = (int)((insn >> 12) & 511) - ((insn & (1u << 20)) ? 512 : 0);
        if (bytes != unit || opc > 1 || (insn & 31) != 3 || offset != direction * (int)bytes)
            return false;
    } else {
        return false;
    }
    /* FEX MemCpy uses x1 for the destination and x2 for the source. A failed
     * post-indexed access hasn't written back its base; a store's preceding
     * load has already advanced x2. For reverse vector copies the pointers
     * address the bottom of the 32-byte chunk, rather than its first x86 unit. */
    if (((insn >> 5) & 31) != (load ? 2u : 1u)) return false;
    const int adjustment = direction < 0 && bytes == 32 ? 32 - (int)unit : 0;
    const uint64_t current_src = host[2] - membase + adjustment - (load ? 0 : direction * (int)bytes);
    const uint64_t current_dst = host[1] - membase + adjustment;
    if (current_src > UINT32_MAX || current_dst > UINT32_MAX) return false;
    const int64_t delta = direction * ((int64_t)current_src - *src);
    if (delta < 0 || delta != direction * ((int64_t)current_dst - *dst) || delta % unit)
        return false;
    const uint64_t completed = (uint64_t)delta / unit;
    if (!completed || completed >= *count) return false;
    *src = (uint32_t)current_src;
    *dst = (uint32_t)current_dst;
    *count -= (uint32_t)completed;
    return true;
}
