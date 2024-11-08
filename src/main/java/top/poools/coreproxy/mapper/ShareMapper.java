package top.poools.coreproxy.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.poools.coreproxy.model.Share;
import top.poools.msapi.kafka.ShareEntity;
import top.poools.msapi.proxy.dto.ShareDto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ShareMapper {

    Share toShare(ShareEntity shareEntity);

    ShareEntity toKafka(Share share);

    Share toEntity(ShareDto shareDto);

    @Mapping(target="connectionId", source="connectionId")
    ShareDto toDto(Share share, String connectionId);
}