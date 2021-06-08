package com.template.calenderproject.model

data class RecurringPattern (
     var eventId :Int?=null,
     var pattern: String?=null,
     var monthOfYear :Int?=null,
     var dayOfMonth :Int?=null,
     var dayOfWeek :Int?=null,
)