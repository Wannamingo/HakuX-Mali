#include "native/core/xbe_media.h"
#include <assert.h>
#include <stdio.h>
#include <string.h>

enum { HEADER_SIZE = 0x500, CERT = 0x200, SECURITY = CERT + 0x1D8 };

static void put32(uint8_t *p, uint32_t v)
{
    for (unsigned i = 0; i < 4; i++) {
        p[i] = (uint8_t)(v >> (i * 8));
    }
}

static void setup(uint8_t *hdr)
{
    memset(hdr, 0xA5, HEADER_SIZE);
    put32(hdr, 0x48454258);
    put32(hdr + 0x104, 0x10000);
    put32(hdr + 0x118, 0x10000 + CERT);
    put32(hdr + CERT, 0x1EC);
}

static void unchanged(uint8_t *hdr, uint32_t size)
{
    uint8_t before[HEADER_SIZE];
    memcpy(before, hdr, sizeof(before));
    assert(!nc_xbe_file_media(hdr, size));
    assert(memcmp(before, hdr, sizeof(before)) == 0);
}

int main(void)
{
    uint8_t hdr[HEADER_SIZE], expected[HEADER_SIZE];
    setup(hdr);
    memcpy(expected, hdr, sizeof(expected));
    expected[SECURITY] &= 0xFE;
    assert(nc_xbe_file_media(hdr, sizeof(hdr)));
    assert(memcmp(hdr, expected, sizeof(hdr)) == 0); /* only bit 0 changes */
    unchanged(hdr, sizeof(hdr));                  /* idempotent */

    setup(hdr);
    put32(hdr + CERT, 0x1D0);                    /* early certificate */
    unchanged(hdr, sizeof(hdr));
    put32(hdr + CERT, 0x1D8);                    /* flag not declared */
    unchanged(hdr, sizeof(hdr));
    put32(hdr + CERT, 0x1DB);                    /* partial flag */
    unchanged(hdr, sizeof(hdr));
    put32(hdr + CERT, 0x1DC);                    /* complete field, no key */
    assert(nc_xbe_file_media(hdr, sizeof(hdr)));

    setup(hdr);
    unchanged(hdr, SECURITY + 3);                /* actual header truncated */
    put32(hdr + CERT, 0xFFFFFFFF);               /* corrupt declared size */
    unchanged(hdr, sizeof(hdr));

    setup(hdr);
    put32(hdr + 0x118, 0xFFFF);                  /* before image base */
    unchanged(hdr, sizeof(hdr));
    put32(hdr + 0x118, 0xFFFFFFFF);              /* outside loaded header */
    unchanged(hdr, sizeof(hdr));
    put32(hdr + 0x118, 0x10000 + HEADER_SIZE);    /* at end */
    unchanged(hdr, sizeof(hdr));
    put32(hdr + 0x118, 0x10000 + HEADER_SIZE - 3);/* partial size field */
    unchanged(hdr, sizeof(hdr));

    setup(hdr);
    unchanged(hdr, 0x11B);                       /* short XBE header */
    put32(hdr, 0);                              /* wrong magic */
    unchanged(hdr, sizeof(hdr));
    assert(!nc_xbe_file_media(NULL, sizeof(hdr)));
    puts("PASS file media policy, bit preservation, idempotence, old and malformed certificates");
    return 0;
}
