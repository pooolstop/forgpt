package top.poools.coreproxy.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.poools.coreproxy.model.ConnectionInfo;
import top.poools.msapi.proxy.dto.ConnectionDto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ConnectionMapper {

    ConnectionInfo toEntity(ConnectionDto connectionDto);

    ConnectionDto toDto(ConnectionInfo connectionInfo);
}