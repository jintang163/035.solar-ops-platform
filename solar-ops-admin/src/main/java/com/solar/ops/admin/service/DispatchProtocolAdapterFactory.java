package com.solar.ops.admin.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DispatchProtocolAdapterFactory {

    @Autowired(required = false)
    @Qualifier("iec104Adapter")
    private DispatchProtocolAdapter iec104Adapter;

    @Autowired(required = false)
    @Qualifier("modbusAdapter")
    private DispatchProtocolAdapter modbusAdapter;

    public DispatchProtocolAdapter getAdapter(Integer protocolType) {
        if (protocolType == null) {
            return getDefaultAdapter();
        }
        switch (protocolType) {
            case 1:
                return iec104Adapter != null ? iec104Adapter : getDefaultAdapter();
            case 2:
                return modbusAdapter != null ? modbusAdapter : getDefaultAdapter();
            default:
                return getDefaultAdapter();
        }
    }

    private DispatchProtocolAdapter getDefaultAdapter() {
        return iec104Adapter != null ? iec104Adapter : modbusAdapter;
    }
}
