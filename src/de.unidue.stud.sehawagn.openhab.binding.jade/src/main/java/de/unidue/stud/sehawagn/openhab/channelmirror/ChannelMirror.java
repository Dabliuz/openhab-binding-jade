package de.unidue.stud.sehawagn.openhab.channelmirror;

import org.eclipse.smarthome.core.thing.ChannelUID;

public interface ChannelMirror {
    void mirrorChannel(ChannelUID inputChannel, ChannelMirrorReceiver channelMirrorRecevier);

    void unMirrorChannel(ChannelUID sourceChannel, ChannelMirrorReceiver updateMonitorRecevier);
}