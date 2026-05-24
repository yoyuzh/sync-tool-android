package com.yoyuzh.cliplink.domain.model

enum class StorageMode {
    /** Full text/content is stored inline in the record. */
    INLINE,

    /** Content is stored as a blob referenced by the record. */
    BLOB,

    /** Only metadata is stored; content has been discarded or not transferred. */
    METADATA_ONLY
}
