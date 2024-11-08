package top.poools.coreproxy.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.poools.msapi.lk.dto.PoolDto;
import top.poools.coreproxy.model.Pool;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PoolMapper {

    Pool toEntity(PoolDto poolDto);

    PoolDto toDto(Pool pool);
}