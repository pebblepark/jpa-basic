package domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
public class Team {

    @Id @GeneratedValue
    Long teamId;

    String teamName;

    @OneToMany(mappedBy = "team")
    Set<Member> members = new HashSet<>();
}
