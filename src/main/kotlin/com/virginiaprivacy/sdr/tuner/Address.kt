package com.virginiaprivacy.sdr.tuner

enum class Address(val mAddress: Int) {
    USB_SYSCTL(8192), USB_CTRL(8208), USB_STAT(8212), USB_EPA_CFG(8516), USB_EPA_CTL(8520), USB_EPA_MAXPKT(8536), USB_EPA_MAXPKT_2(
        8538
    ),
    USB_EPA_FIFO_CFG(8544), DEMOD_CTL(12288), GPO(12289), GPI(12290), GPOE(12291), GPD(12292), SYSINTE(12293), SYSINTS(
        12294
    ),
    GP_CFG0(12295), GP_CFG1(12296), SYSINTE_1(12297), SYSINTS_1(12298), DEMOD_CTL_1(12299), IR_SUSPEND(12300);

    val address: Short
        get() = mAddress.toShort()
}