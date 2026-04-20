package com.kapi.ledgerroast.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun txTypeToString(value: TxType): String = value.name

    @TypeConverter
    fun stringToTxType(value: String): TxType = TxType.valueOf(value)
}
