package com.template.calenderproject

class ChangeCalender {
    var list = listOf(
        "April",
        "May",
        "June",
        "July",
        "August",
        "September",
        "October",
        "November",
        "December",
        "January",
        "February",
        "March"
    )


    fun changeToPersian(mouth: String):String {

       return when (mouth) {
            list[0] -> "فروردین"
            list[1] -> "اردیبهشت"
            list[2] -> "خرداد"
            list[3] -> "تیر"
            list[4] -> "مرداد"
            list[5] -> "شهریور"
            list[6] -> "مهر"
            list[7] -> "آبان"
            list[8]  -> "آذر"
            list[9] -> "دی"
            list[10] -> "بهمن"
            list[11] -> "اسفند"
           else ->  "Noting"
       }

    }


}