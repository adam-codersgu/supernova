package com.codersguidebook.supernova.testutils

import java.lang.reflect.Field
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.isAccessible

object ReflectionUtils {

    // TODO MIGRATE TO KOTLIN REFLECTION? SEE IF THERE IS A DEPENDENCY TO DELETE
    fun setFieldVisible(targetObject: Any, fieldName: String): Field {
        val targetField = targetObject.javaClass.getDeclaredField(fieldName)
        targetField.isAccessible = true
        return targetField
    }

    fun setMethodVisible(targetObject: Any, methodName: String): KFunction<*> {
        val targetMethod = targetObject::class.declaredFunctions
            .first { it.name == methodName }
        targetMethod.isAccessible = true
        return targetMethod
    }

    fun replaceFieldWithMock(targetObject: Any, fieldName: String, mockObject: Any) {
        val field = setFieldVisible(targetObject, fieldName)
        field.set(targetObject, mockObject)
    }
}