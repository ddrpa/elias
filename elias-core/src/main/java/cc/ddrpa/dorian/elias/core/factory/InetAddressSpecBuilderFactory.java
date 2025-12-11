package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Factory for handling InetAddress types and converting them to appropriate BINARY types.
 * <p>
 * IPv4 addresses are stored as BINARY(4) - 4 bytes
 * IPv6 addresses are stored as BINARY(16) - 16 bytes
 * Generic InetAddress is stored as VARBINARY(16) to accommodate both IPv4 and IPv6
 */
public class InetAddressSpecBuilderFactory implements SpecBuilderFactory {

    private static final List<String> INET4_ADDRESS_TYPES = List.of(
            "java.net.Inet4Address"
    );

    private static final List<String> INET6_ADDRESS_TYPES = List.of(
            "java.net.Inet6Address"
    );

    private static final List<String> INET_ADDRESS_TYPES = List.of(
            "java.net.InetAddress"
    );

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return INET4_ADDRESS_TYPES.contains(fieldTypeName)
                || INET6_ADDRESS_TYPES.contains(fieldTypeName)
                || INET_ADDRESS_TYPES.contains(fieldTypeName);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        String fieldType = field.getType().getName();
        ColumnSpecBuilder builder = SpecBuilderFactory.super.builder(field);

        if (INET4_ADDRESS_TYPES.contains(fieldType)) {
            return builder.setDataType("binary").setLength(4L);
        } else if (INET6_ADDRESS_TYPES.contains(fieldType)) {
            return builder.setDataType("binary").setLength(16L);
        } else {
            return builder.setDataType("varbinary").setLength(16L);
        }
    }
}
