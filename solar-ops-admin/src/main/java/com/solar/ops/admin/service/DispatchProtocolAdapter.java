package com.solar.ops.admin.service;

import com.solar.ops.admin.entity.GridDispatchUploadRecord;
import com.solar.ops.admin.entity.GridDispatchCommand;
import com.solar.ops.admin.vo.GridDispatchProtocolConfigVO;

import java.util.List;

public interface DispatchProtocolAdapter {

    String getProtocolType();

    boolean connect(GridDispatchProtocolConfigVO config);

    void disconnect();

    boolean isConnected();

    boolean uploadData(GridDispatchUploadRecord record);

    List<GridDispatchCommand> receiveCommands();

    boolean sendCommandResponse(GridDispatchCommand command, boolean success, String reason);
}
