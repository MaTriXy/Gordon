package com.banno.gordon

import arrow.fx.IO
import arrow.fx.extensions.fx
import se.vidstige.jadb.JadbConnection
import se.vidstige.jadb.JadbDevice

internal typealias PoolName = String

internal data class DevicePool(
    val poolName: PoolName,
    val devices: List<JadbDevice>
)

internal fun calculatePools(adb: JadbConnection, strategy: PoolingStrategy): IO<List<DevicePool>> = IO.fx {
    val allDevices = adb.getAllDevices().bind()

    when (strategy) {
        PoolingStrategy.EachDevice,
        PoolingStrategy.PoolPerDevice -> allDevices.map { DevicePool(it.serial, listOf(it)) }

        PoolingStrategy.AllDevices,
        PoolingStrategy.SinglePool -> listOf(DevicePool("All-Devices", allDevices))

        PoolingStrategy.PhonesAndTablets -> {
            val deviceAndIsTablet = allDevices.map { it to it.isTablet().bind() }

            listOf(
                DevicePool("Tablets", deviceAndIsTablet.filter { it.second }.map { it.first }),
                DevicePool("Phones", deviceAndIsTablet.filter { !it.second }.map { it.first })
            )
        }

        is PoolingStrategy.Manual -> strategy.poolNameToDeviceSerials.map { (poolName, deviceSerials) ->
            DevicePool(poolName, deviceSerials.mapNotNull { serial -> allDevices.find { it.serial == serial } })
        }

        is PoolingStrategy.SpecificDevices -> strategy.deviceSerials
            .map { serial -> DevicePool(serial, listOfNotNull(allDevices.find { it.serial == serial })) }
    }
}
