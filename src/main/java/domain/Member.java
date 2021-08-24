package domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
public class Member extends BaseEntity{

    @Id
    @GeneratedValue
    private Long memberId;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private Team team;

    @ElementCollection
    @CollectionTable(name = "favorite_food", joinColumns =
        @JoinColumn(name = "member_id"))
    @Column(name = "food_name")
    private Set<String> favoriteFoods = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "home_address", joinColumns =
    @JoinColumn(name = "member_id"))
    private Set<domain.Address> addresses = new HashSet<>();

    @Override
    public String toString() {
        return "domain.Member{" +
                "memberId=" + memberId +
                ", name='" + name + '\'' +
                ", team=" + team +
                '}';
    }
}
