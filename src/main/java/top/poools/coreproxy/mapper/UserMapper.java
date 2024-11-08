package top.poools.coreproxy.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.poools.msapi.lk.dto.UserDto;
import top.poools.coreproxy.model.User;

@Mapper(componentModel = "spring",
        uses = {
                MinerMapper.class
        },
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    User toEntity(UserDto userDto);

    UserDto toDto(User user);
}