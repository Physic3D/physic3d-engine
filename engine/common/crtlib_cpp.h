#ifndef CRTLIB_CPP_H
#define CRTLIB_CPP_H

#ifdef __cplusplus

// Must be included AFTER crtlib.h (for _Mem_Alloc/_Mem_Realloc prototypes)
// and after port.h (for byte typedef).

#include <algorithm>
#include <cstddef>

template<typename T, typename U>
inline auto min(T a, U b) -> decltype(a < b ? a : b) { return (a < b) ? a : b; }

template<typename T, typename U>
inline auto max(T a, U b) -> decltype(a < b ? a : b) { return (a > b) ? a : b; }

struct MemAllocResult {
	void* pool;
	size_t size;
	const char* file;
	int line;

	MemAllocResult(void* pool_, size_t size_, const char* file_, int line_)
		: pool(pool_), size(size_), file(file_), line(line_) {}

	template<typename T>
	operator T*() {
		return static_cast<T*>(_Mem_Alloc(static_cast<byte*>(pool), size, file, line));
	}

	operator void*() {
		return _Mem_Alloc(static_cast<byte*>(pool), size, file, line);
	}
};

struct MemReallocResult {
	void* pool;
	void* ptr;
	size_t size;
	const char* file;
	int line;

	MemReallocResult(void* pool_, void* ptr_, size_t size_, const char* file_, int line_)
		: pool(pool_), ptr(ptr_), size(size_), file(file_), line(line_) {}

	template<typename T>
	operator T*() {
		return static_cast<T*>(_Mem_Realloc(static_cast<byte*>(pool), ptr, size, file, line));
	}

	operator void*() {
		return _Mem_Realloc(static_cast<byte*>(pool), ptr, size, file, line);
	}
};

#define Mem_Alloc( pool, size ) MemAllocResult(pool, static_cast<size_t>(size), __FILE__, __LINE__)
#define Mem_Realloc( pool, ptr, size ) MemReallocResult(pool, ptr, static_cast<size_t>(size), __FILE__, __LINE__)

template<typename T>
inline T* Mem_AllocTyped(void* pool, size_t size, const char* file, int line) {
	return static_cast<T*>(_Mem_Alloc(static_cast<byte*>(pool), size, file, line));
}
#define Mem_AllocT(type, pool, size) Mem_AllocTyped<type>(pool, size, __FILE__, __LINE__)

#endif // __cplusplus

#endif // CRTLIB_CPP_H