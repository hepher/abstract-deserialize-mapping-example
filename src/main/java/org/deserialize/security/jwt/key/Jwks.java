package com.enelx.bfw.framework.security.jwt.key;

import com.enelx.bfw.framework.entity.AbstractEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Jwks extends AbstractEntity {

    private List<Jwk> keys;

    public void addKey(Jwk jwk) {
        if (keys == null) {
            keys = new ArrayList<>();
        }
        keys.add(jwk);
    }
}
