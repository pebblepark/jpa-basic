package domain.item;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn
@Getter @Setter
public class Item {

    @Id @GeneratedValue
    private Long id;

    private String name;
    private int price;
}
