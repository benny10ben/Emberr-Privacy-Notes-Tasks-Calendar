package com.emberr.domain.util.network

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalNetworkHostValidatorTest {

    private fun assertTreatedAsLocal(host: String) =
        assertTrue(LocalNetworkHostValidator.isLocalNetworkHost(host), "$host should be treated as local")

    private fun assertTreatedAsPublic(host: String) =
        assertFalse(LocalNetworkHostValidator.isLocalNetworkHost(host), "$host should be treated as public")

    @Test
    fun localhostIsLocalWhateverWayItIsCapitalised() {
        assertTreatedAsLocal("localhost")
        assertTreatedAsLocal("LOCALHOST")
        assertTreatedAsLocal("LocalHost")
    }

    @Test
    fun theLoopbackRangeIsLocal() {
        assertTreatedAsLocal("127.0.0.1")
        assertTreatedAsLocal("127.1.2.3")
        assertTreatedAsLocal("127.255.255.255")
    }

    @Test
    fun thePrivateTenRangeIsLocal() {
        assertTreatedAsLocal("10.0.0.1")
        assertTreatedAsLocal("10.255.255.255")
    }

    @Test
    fun thePrivateHomeRouterRangeIsLocal() {
        assertTreatedAsLocal("192.168.0.1")
        assertTreatedAsLocal("192.168.1.5")
        assertTreatedAsLocal("192.168.255.255")
    }

    @Test
    fun onlyTheSixteenToThirtyOneSliceOfTheOneSeventyTwoRangeIsLocal() {
        assertTreatedAsLocal("172.16.0.1")
        assertTreatedAsLocal("172.31.255.255")
        assertTreatedAsPublic("172.15.0.1")
        assertTreatedAsPublic("172.32.0.1")
    }

    @Test
    fun addressesThatOnlyLookPrivateAreStillPublic() {
        assertTreatedAsPublic("11.0.0.1")
        assertTreatedAsPublic("192.169.0.1")
        assertTreatedAsPublic("193.168.0.1")
        assertTreatedAsPublic("8.8.8.8")
        assertTreatedAsPublic("1.2.3.4")
    }

    @Test
    fun malformedNumericAddressesAreRejected() {
        assertTreatedAsPublic("10.0.0")
        assertTreatedAsPublic("10.0.0.1.5")
        assertTreatedAsPublic("10.0.0.256")
        assertTreatedAsPublic("10.0.0.-1")
        assertTreatedAsPublic("10.0.0.x")
        assertTreatedAsPublic("")
    }

    @Test
    fun ordinaryHostNamesAreNotLocal() {
        assertTreatedAsPublic("example.com")
        assertTreatedAsPublic("cloud.example.com")
        assertTreatedAsPublic("notlocalhost")
    }

    @Test
    fun theSixLoopbackAddressIsLocalWithOrWithoutBrackets() {
        assertTreatedAsLocal("::1")
        assertTreatedAsLocal("[::1]")
        assertTreatedAsLocal("0:0:0:0:0:0:0:1")
    }

    @Test
    fun theSixLinkLocalRangeIsLocal() {
        assertTreatedAsLocal("fe80::1")
        assertTreatedAsLocal("fe80::abcd:1234")
        assertTreatedAsLocal("febf::1")
        assertTreatedAsPublic("fec0::1")
    }

    @Test
    fun theSixUniqueLocalRangeIsLocal() {
        assertTreatedAsLocal("fc00::1")
        assertTreatedAsLocal("fd12:3456::1")
        assertTreatedAsLocal("fdff::1")
        assertTreatedAsPublic("fe00::1")
    }

    @Test
    fun publicSixAddressesAreNotLocal() {
        assertTreatedAsPublic("2001:db8::1")
        assertTreatedAsPublic("[2001:db8::1]")
        assertTreatedAsPublic("2606:4700:4700::1111")
    }

    @Test
    fun theUnspecifiedSixAddressIsNotTreatedAsLocal() {
        assertTreatedAsPublic("::")
    }

    @Test
    fun malformedSixAddressesAreRejected() {
        assertTreatedAsPublic("1::2::3")
        assertTreatedAsPublic("1:2:3:4:5:6:7:8:9")
        assertTreatedAsPublic("1:2:3:4:5:6:7")
        assertTreatedAsPublic("fe80::gggg")
        assertTreatedAsPublic("fe80::12345")
    }

    @Test
    fun anExtraColonIsNeverMistakenForTheLoopbackAddress() {
        assertTreatedAsPublic(":::1")
        assertTreatedAsPublic("::1:")
        assertTreatedAsPublic(":1:2:3:4:5:6:7:8")
        assertTreatedAsPublic("fe80:::1")
        assertTreatedAsPublic("fe80:")
    }

    @Test
    fun aCompressedRangeStillCountsWhenItSitsAtTheEnd() {
        assertTreatedAsLocal("fe80::")
        assertTreatedAsLocal("fd00::")
    }
}
