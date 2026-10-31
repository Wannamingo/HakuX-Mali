#ifndef NATIVE_CORE_MOVIE_H
#define NATIVE_CORE_MOVIE_H
#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>
void nc_movie_enable(bool enabled);
uint64_t nc_movie_begin(const void *file, const char *path);
void nc_movie_read(uint64_t token, uint64_t offset, const void *data, size_t size);
void nc_movie_close(const void *file);
void nc_movie_reset(void);
bool nc_movie_source(uint32_t *width, uint32_t *height);
#endif
