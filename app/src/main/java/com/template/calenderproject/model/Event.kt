package com.template.calenderproject.model

data class Event(
    var id: Int? = null,
    var title: String? = null,
    var isAllDay: Boolean? = null,
    var date: String? = null,
    var time: String? = null,
    var month: String? = null,
    var year: String? = null,
    var duration: String? = null,
    var isNotify: Boolean? = null,
    var isRecurring: Boolean? = null,
    var recurringPeriod: String? = null,
    var note: String? = null,
    var color: Int? = null,
    var phoneNumber: String? = null,
    var parentId: Int? = null,
)
