package com.virginiaprivacy.drivers.sdr.usb

data class ControlTransferResult(
    val direction: ControlTransferDirection,
    val status: ResultStatus,
    val packet: ControlPacket
) {

    fun getData(): Int {
        when (direction) {
            ControlTransferDirection.IN -> return packet.getTransferredData().run {
                when (this.size) {
                    1 -> return@run this[0].toInt()
                    else -> return@run (this[1].toInt() shl 8 or this[0].toInt())
                }
            }
            else -> return packet.getTransferredData().size
        }
    }


}

@JvmInline
value class ControlPacket(private val bytes: ByteArray) {
    fun getTransferredData(): ByteArray = bytes.copyOf()
}


enum class ControlTransferDirection {
    IN,
    OUT;

    companion object {
        fun get(code: Byte): ControlTransferDirection = if (code == 128.toByte()) IN else OUT
    }
}

sealed class ResultStatus {
    abstract class Completed : ResultStatus()
    abstract class Error : ResultStatus()

    object TransferStall : Error()

    object Timeout : Error()

    object OtherError : Error()

    object Success : Completed()

}
