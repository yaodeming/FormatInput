package com.dpuntu.editui

import android.text.InputFilter
import android.text.Spanned
import android.util.Log

/**
 * Created  by fangmingxing on 2018/5/18.
 */
object UtilEditTextFilter {
    private val TAG = UtilEditTextFilter.javaClass.simpleName

    private const val INPUT_INT_ARRAY = "0123456789"
    private const val INPUT_STR_ARRAY = "abcdefghigklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

    const val EMPTY = " "
    const val DELIMIT = "-"
    const val MONEY_PRE = "¥"
    const val DECIMAL_POINT = "."
    const val BLANK = ""
    const val ZERO = "0"
    const val COMMA = ","

    class PinPwdFormatterFilter(private val maxLength: Int) : InputFilter {
        override fun filter(source: CharSequence?, start: Int, end: Int, dest: Spanned?, dstart: Int, dend: Int): CharSequence {
            val newChar = source?.toString() ?: BLANK
            return if (dest.toString().length < maxLength) {
                if (newChar.all { it.toString() in INPUT_INT_ARRAY }) newChar else BLANK
            } else BLANK
        }
    }

    abstract class AbsInputFilter(private val inputType: Int) : InputFilter {

        fun inputType() = inputType

        abstract fun inputFilter(newChar: String, oldString: String): String

        abstract fun isValidInput(newChar: String): Boolean

        /**
         * @param source 输入的文字，删除为空
         * @param start  输入=0，删除=0
         * @param end    输入=source文字的长度，删除=0
         * @param dest   原先显示的内容
         * @param dstart 输入=原光标位置，删除=光标删除结束位置
         * @param dend   输入=原光标位置，删除=光标删除开始位置
         * @return
         */
        override fun filter(source: CharSequence?, start: Int, end: Int, dest: Spanned?, dstart: Int, dend: Int): CharSequence {
            Log.d(TAG, "source = $source , start = $start , end = $end , dest = $dest , dstart = $dstart , dend = $dend")
            val newChar = source?.toString() ?: BLANK
            var oldString = dest?.toString() ?: BLANK
            if (end == 0 && dend == dstart + 1) return BLANK // 删除操作
            if (!isValidInput(newChar)) return BLANK // 粘贴前检查合法性
            // 支持粘贴时格式化
            var returnStr = BLANK
            newChar.forEach {
                val charInput = inputFilter(it.toString(), oldString)
                returnStr += charInput
                oldString += charInput
            }
            return returnStr
        }
    }

    abstract class NumberFilter(private val inputType: Int,
                                private val isSupportFirstZero: Boolean = false,
                                private val isSupportDecimal: Boolean = false,
                                private val maxIntegerLength: Int = 7,
                                private val maxDecimalLength: Int = 2) : AbsInputFilter(inputType) {

        private val maxLength = maxIntegerLength + if (isSupportDecimal) maxDecimalLength + 1 else 0

        fun maxIntegerLength() = maxIntegerLength
        fun maxDecimalLength() = maxDecimalLength
        fun isSupportDecimal() = isSupportDecimal
        fun isSupportFirstZero() = isSupportFirstZero

        override fun isValidInput(newChar: String): Boolean {
            return when (inputType) {
                CustomFormatEditText.INPUT_INTEGER -> integerValidInput(newChar)
                CustomFormatEditText.INPUT_DECIMAL -> decimalValidInput(newChar)
                CustomFormatEditText.INPUT_MONEY -> moneyValidInput(newChar)
                else -> true
            }
        }

        private fun integerValidInput(newChar: String): Boolean = newChar.all { it in INPUT_INT_ARRAY }

        private fun decimalValidInput(newChar: String): Boolean = newChar.all { it in INPUT_INT_ARRAY + DECIMAL_POINT }

        private fun moneyValidInput(newChar: String): Boolean = newChar.all { it in MONEY_PRE + INPUT_INT_ARRAY + DECIMAL_POINT }

        override fun inputFilter(newChar: String, oldString: String): String {
            return when (inputType) {
                CustomFormatEditText.INPUT_INTEGER -> integerFilter(newChar, oldString)
                CustomFormatEditText.INPUT_DECIMAL -> decimalFilter(newChar, oldString)
                CustomFormatEditText.INPUT_MONEY -> moneyFilter(newChar, oldString)
                else -> BLANK
            }
        }

        private fun integerFilter(newChar: String, oldString: String): String =
                when {
                    !isSupportFirstZero && newChar == ZERO -> BLANK
                    isSupportFirstZero && oldString == ZERO && newChar == ZERO -> BLANK
                    else -> if (oldString.length < maxLength) {
                        if (newChar in INPUT_INT_ARRAY) newChar else BLANK
                    } else BLANK
                }

        private fun decimalFilter(newChar: String, oldString: String): String =
                if (!oldString.contains(DECIMAL_POINT)) {
                    if (oldString == ZERO && newChar != DECIMAL_POINT) BLANK
                    else {
                        if (oldString.length < maxIntegerLength) {
                            if (newChar in (INPUT_INT_ARRAY + DECIMAL_POINT)) {
                                if (oldString.isBlank() && newChar == DECIMAL_POINT) BLANK else newChar
                            } else BLANK
                        } else if (oldString.length == maxIntegerLength && newChar == DECIMAL_POINT) newChar else BLANK
                    }
                } else {
                    val afterDecLen = oldString.length - (oldString.indexOf(DECIMAL_POINT) + 1)
                    if (afterDecLen < maxDecimalLength) {
                        if (newChar in INPUT_INT_ARRAY || newChar != DECIMAL_POINT) newChar else BLANK
                    } else BLANK
                }

        private fun moneyFilter(newChar: String, oldString: String): String =
                if (isSupportDecimal) {
                    if (!oldString.contains(DECIMAL_POINT)) {
                        if (oldString.replace(MONEY_PRE, BLANK) == ZERO && newChar != DECIMAL_POINT) BLANK
                        else {
                            when {
                                oldString.isBlank() && newChar in (INPUT_INT_ARRAY + DECIMAL_POINT) && newChar != DECIMAL_POINT -> MONEY_PRE + newChar
                                oldString.isBlank() && newChar.startsWith(MONEY_PRE) && newChar in (INPUT_INT_ARRAY + MONEY_PRE + DECIMAL_POINT) -> newChar
                                oldString.replace(MONEY_PRE, BLANK).length < maxIntegerLength -> {
                                    if (newChar in (INPUT_INT_ARRAY + DECIMAL_POINT)) {
                                        if (oldString.replace(MONEY_PRE, BLANK).isBlank() && newChar == DECIMAL_POINT) BLANK else newChar
                                    } else BLANK
                                }
                                else -> if (oldString.replace(MONEY_PRE, BLANK).length == maxIntegerLength && newChar == DECIMAL_POINT) newChar else BLANK
                            }
                        }
                    } else {
                        val afterDecLen = oldString.replace(MONEY_PRE, BLANK).length - (oldString.replace(MONEY_PRE, BLANK).indexOf(DECIMAL_POINT) + 1)
                        if (afterDecLen < maxDecimalLength) {
                            if (newChar in INPUT_INT_ARRAY || newChar != DECIMAL_POINT) newChar else BLANK
                        } else BLANK
                    }
                } else {
                    when {
                        oldString.isBlank() && newChar == ZERO -> BLANK
                        oldString.isBlank() && newChar in INPUT_INT_ARRAY -> MONEY_PRE + newChar
                        oldString.isBlank() && newChar.startsWith(MONEY_PRE) && newChar in (INPUT_INT_ARRAY + MONEY_PRE) -> newChar
                        oldString.replace(MONEY_PRE, BLANK).length < maxLength -> if (newChar in INPUT_INT_ARRAY) newChar else BLANK
                        else -> BLANK
                    }
                }
    }

    /**
     * 整形格式化输入处理
     *
     * @param maxIntegerLength 整形金额的长度
     * */
    class IntegerFilter(maxIntegerLength: Int = 7, isSupportFirstZero: Boolean = false) : NumberFilter(CustomFormatEditText.INPUT_INTEGER, isSupportFirstZero = isSupportFirstZero, maxIntegerLength = maxIntegerLength)

    /**
     * 小数格式化输入处理
     *
     * @param maxIntegerLength 整形金额的长度
     * @param maxDecimalLength 小数点后金额的长度
     * */
    class DecimalFilter(maxIntegerLength: Int = 7,
                        maxDecimalLength: Int = 2) : NumberFilter(CustomFormatEditText.INPUT_DECIMAL, isSupportDecimal = true, maxIntegerLength = maxIntegerLength, maxDecimalLength = maxDecimalLength)

    /**
     * 金额格式化输入处理
     *
     * @param isSupportDecimal 是否支持小数金额
     * @param maxIntegerLength 整形金额的长度
     * @param maxDecimalLength 小数点后金额的长度
     * */
    class MoneyFilter(isSupportDecimal: Boolean = true,
                      maxIntegerLength: Int = 7,
                      maxDecimalLength: Int = 2) : NumberFilter(CustomFormatEditText.INPUT_MONEY, isSupportDecimal = isSupportDecimal, maxIntegerLength = maxIntegerLength, maxDecimalLength = maxDecimalLength)

    /**
     * 电话号码格式化输入处理
     *
     * @param isOnlyMobilePhone 是否只支持移动电话
     * @param isSupportDelimit 是否支持使用区号分割符
     * @param delimitString 区号分割符
     * @param isSupportFormatter 是否需要格式化
     * @param formatterString 格式化时候的分隔符
     * */
    class PhoneFilter(private val isOnlyMobilePhone: Boolean = true,
                      private val isSupportDelimit: Boolean = false,
                      private val delimitString: String = DELIMIT,
                      private val isSupportFormatter: Boolean = false,
                      private val formatterString: String = EMPTY,
                      private val maxLength: Int = 11) : AbsInputFilter(CustomFormatEditText.INPUT_PHONE) {

        override fun isValidInput(newChar: String): Boolean = newChar.all { it in INPUT_INT_ARRAY + delimitString + formatterString }

        fun isOnlyMobilePhone() = isOnlyMobilePhone
        fun isSupportDelimit() = isSupportDelimit
        fun delimitString() = delimitString
        fun isSupportFormatter() = isSupportFormatter
        fun formatterString() = formatterString

        override fun inputFilter(newChar: String, oldString: String): String {
            val hasDelimit = oldString.contains(delimitString)
            val phoneMaxLength = when (isOnlyMobilePhone) {
                true -> if (isSupportFormatter) maxLength + 2 else maxLength
                false -> if (isSupportDelimit && hasDelimit) maxLength + 1 else maxLength
            }
            return if (oldString.length < phoneMaxLength) {
                when (isOnlyMobilePhone) {
                    true -> {
                        if (newChar in INPUT_INT_ARRAY) {
                            when (isSupportFormatter) {
                                true -> if (oldString.length == 3 || oldString.length == 8) formatterString + newChar else newChar
                                else -> newChar
                            }
                        } else BLANK
                    }
                    false -> {
                        if (newChar in INPUT_INT_ARRAY || newChar == delimitString) {
                            when (isSupportDelimit) {
                                true -> if (hasDelimit && newChar == delimitString) BLANK else newChar
                                false -> if (newChar == delimitString) BLANK else newChar
                            }
                        } else BLANK
                    }
                }
            } else BLANK
        }
    }

    /**
     * 分组码格式化输入处理
     *
     * @param inputGroupType 分组输入类型
     * @param supportGroupSize 支持最大分组个数
     * @param groupItemLength 每个分组的长度
     * @param groupItemString 组与组之间的分隔符
     * */
    class GroupFilter(inputGroupType: Int = -1,
                      private val supportGroupSize: Int = 4,
                      private val groupItemLength: Int = 4,
                      private val groupItemString: String = EMPTY) : AbsInputFilter(CustomFormatEditText.INPUT_GROUP) {

        private val maxLength = (groupItemLength + 1) * supportGroupSize - 1

        fun supportGroupSize() = supportGroupSize
        fun groupItemLength() = groupItemLength
        fun groupItemString() = groupItemString

        val inputType = when (inputGroupType) {
            CustomFormatEditText.GROUP_LETTER -> INPUT_STR_ARRAY
            CustomFormatEditText.GROUP_NUMBER -> INPUT_INT_ARRAY
            CustomFormatEditText.GROUP_ALL -> INPUT_INT_ARRAY + INPUT_STR_ARRAY
            else -> INPUT_INT_ARRAY + INPUT_STR_ARRAY
        }

        override fun isValidInput(newChar: String): Boolean = newChar.all { it in inputType + groupItemString }

        override fun inputFilter(newChar: String, oldString: String): String {
            return if (oldString.length < maxLength && newChar in inputType) {
                if (oldString.isNotBlank() && oldString.replace(groupItemString, BLANK).length % groupItemLength == 0) groupItemString + newChar else newChar
            } else BLANK
        }
    }

    /**
     * 积分格式化输入处理
     *
     * @param maxLength 积分最大长度
     * @param pointItemLength 每个分组的长度
     * @param formatterString 组与组之间的分隔符
     * */
    class PointFilter(private val maxLength: Int = 9,
                      private val pointItemLength: Int = 3,
                      private val formatterString: String = COMMA) : AbsInputFilter(CustomFormatEditText.INPUT_POINT) {

        override fun isValidInput(newChar: String): Boolean = newChar.all { it in INPUT_INT_ARRAY + formatterString }

        fun maxLength() = maxLength
        fun formatterString() = formatterString
        fun pointItemLength() = pointItemLength

        override fun inputFilter(newChar: String, oldString: String): String {
            return if (oldString.replace(formatterString, BLANK).length < maxLength) {
                when {
                    oldString.isBlank() && newChar == ZERO -> BLANK
                    newChar in (INPUT_INT_ARRAY + formatterString) -> newChar
                    else -> BLANK
                }
            } else BLANK
        }
    }
}