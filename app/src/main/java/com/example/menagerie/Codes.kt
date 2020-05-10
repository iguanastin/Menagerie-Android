package com.example.menagerie

enum class Codes {
    preview_request_storage_perms_for_download,
    preview_request_storage_read_perms_for_download,
    search_request_storage_permissions_for_upload,
    search_activity_result_pick_file_for_upload,
    search_activity_result_settings_closed,
    search_activity_result_authenticate,
    search_activity_result_tags_list_search_tag
}

const val UNKNOWN_SEARCH_TOTAL = -1
const val UNKNOWN_PAGE_SIZE = -1
const val UNKNOWN_PAGE_COUNT = -1

const val PREVIEW_INDEX_IN_PAGE_EXTRA_ID = "com.example.menagerie.preview_index_in_page_extra"
const val PREVIEW_PAGE_EXTRA_ID = "com.example.menagerie.preview_page_extra"
const val PREVIEW_ITEM_EXTRA_ID = "com.example.menagerie.preview_item_extra"
const val PREVIEW_SEARCH_EXTRA_ID = "com.example.menagerie.preview_search_extra"
const val TAGS_LIST_TAG_EXTRA_ID = "com.example.menagerie.tags_list_tag_extra"