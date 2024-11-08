package top.poools.coreproxy.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.poools.msapi.lk.dto.MinerDto;
import top.poools.coreproxy.model.Miner;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MinerMapper {

    Miner toMiner(MinerDto minerDto);

    MinerDto toDto(Miner miner);
}