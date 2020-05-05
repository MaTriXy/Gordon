package com.banno.gordon

import arrow.fx.IO
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.iface.Annotatable
import org.jf.dexlib2.iface.BasicAnnotation
import org.jf.dexlib2.iface.reference.TypeReference
import java.io.File

internal fun loadTestSuite(instrumentationApk: File): IO<List<TestCase>> = IO {
    DexFileFactory
        .loadDexContainer(instrumentationApk, null)
        .run { dexEntryNames.map { getEntry(it)!!.dexFile } }
        .flatMap { it.classes }
        .filter {
            val isKotlinInterfaceDefaultImplementation = it.name.endsWith("\$DefaultImpls")
            val isInterface = (it.accessFlags and AccessFlags.INTERFACE.value != 0)
            val isAbstract = (it.accessFlags and AccessFlags.ABSTRACT.value != 0)

            !isInterface &&
                    !isAbstract &&
                    !isKotlinInterfaceDefaultImplementation
        }
        .flatMap { classDef ->
            classDef.methods
                .mapNotNull { method ->
                    if (method.isTestMethod) {
                        TestCase(
                            fullyQualifiedClassName = classDef.name,
                            methodName = method.name,
                            isIgnored = method.isIgnored || classDef.isIgnored
                        )
                    } else null
                }
        }
}

private val TypeReference.name
    get() = type.drop(1).dropLast(1).replace('/', '.')

private val BasicAnnotation.name
    get() = type.drop(1).dropLast(1).replace('/', '.')

private val Annotatable.isIgnored
    get() = annotations.any { it.name == "org.junit.Ignore" }

private val Annotatable.isTestMethod
    get() = annotations.any { it.name == "org.junit.Test" }
