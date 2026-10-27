/* Runtime media policy for XBEs loaded from a file-backed disc image. */
#ifndef NATIVE_CORE_XBE_MEDIA_H
#define NATIVE_CORE_XBE_MEDIA_H

#include <stdbool.h>
#include <stdint.h>

/* Clear the certificate's physical-disc security-sector requirement in the
 * loaded header only. Older or truncated certificates are left untouched.
 * Returns true when that requirement was present and has been cleared. */
bool nc_xbe_file_media(uint8_t *hdr, uint32_t hdr_len);

#endif
