package com.codersguidebook.supernova.testutils

import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

object ReflectionUtils {

    fun replaceFieldWithMock(targetObject: Any, fieldName: String, mockObject: Any) {
        val property = setFieldVisible(targetObject, fieldName)

        if (property is KMutableProperty<*>) {
            property.setter.call(targetObject, mockObject)
        } else {
            val javaField = property.javaField
                ?: throw IllegalArgumentException("Property $fieldName does not have a backing field to overwrite")

            javaField.isAccessible = true
            javaField.set(targetObject, mockObject)
        }
    }

    fun setFieldVisible(targetObject: Any, fieldName: String): KProperty1<out Any, *> {
        val property = targetObject::class.memberProperties
            .firstOrNull { it.name == fieldName }
            ?: throw NoSuchFieldException("Property $fieldName not found in class hierarchy")

        property.isAccessible = true
        return property
    }

    fun setMethodVisible(targetObject: Any, methodName: String): KFunction<*> {
        val targetMethod = targetObject::class.declaredFunctions
            .first { it.name == methodName }
        targetMethod.isAccessible = true
        return targetMethod
    }
}