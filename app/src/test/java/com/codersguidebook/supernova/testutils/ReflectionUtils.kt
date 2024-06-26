package com.codersguidebook.supernova.testutils

import java.lang.reflect.Field

object ReflectionUtils {

    fun setFieldVisible(targetObject: Any, fieldName: String): Field {
        val targetField = targetObject.javaClass.getDeclaredField(fieldName)
        targetField.isAccessible = true
        return targetField
    }

    fun replaceFieldWithMock(targetObject: Any, fieldName: String, mockObject: Any) {
        val field = setFieldVisible(targetObject, fieldName)
        field.set(targetObject, mockObject)
    }
}