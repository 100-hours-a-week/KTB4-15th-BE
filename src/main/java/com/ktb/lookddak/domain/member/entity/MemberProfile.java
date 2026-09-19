package com.ktb.lookddak.domain.member.entity;

import com.ktb.lookddak.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(
        name = "member_profile",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_profile_member_id",
                columnNames = "member_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(nullable = false, length = 10)
    private String name;

    @Column(nullable = false)
    private Integer age;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal height;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal weight;

    @Column(name = "full_body_image_key", nullable = false, length = 1024)
    private String fullBodyImageKey;

    private MemberProfile(
            Member member,
            String name,
            Integer age,
            BigDecimal height,
            BigDecimal weight,
            String fullBodyImageKey
    ) {
        this.member = member;
        this.name = name;
        this.age = age;
        this.height = height;
        this.weight = weight;
        this.fullBodyImageKey = fullBodyImageKey;
    }

    public static MemberProfile create(
            Member member,
            String name,
            Integer age,
            BigDecimal height,
            BigDecimal weight,
            String fullBodyImageKey
    ) {
        return new MemberProfile(
                member,
                name,
                age,
                height,
                weight,
                fullBodyImageKey
        );
    }
}
