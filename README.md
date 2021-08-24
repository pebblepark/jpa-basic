# JPA

## JPA DB 지정
- `hibernate.dialect` 속성을 이용해서 특정 데이터베이스 설정

### META-INF/persistence.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
    <persistence version="2.2"
    xmlns="http://xmlns.jcp.org/xml/ns/persistence" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence http://xmlns.jcp.org/xml/ns/persistence/persistence_2_2.xsd">

        <persistence-unit name="hello">

        <properties>
            <!-- 필수 속성 -->
            <property name="javax.persistence.jdbc.driver" value="org.h2.Driver"/>
            <property name="javax.persistence.jdbc.user" value="sa"/>
            <property name="javax.persistence.jdbc.password" value=""/>
            <property name="javax.persistence.jdbc.url" value="jdbc:h2:tcp://localhost/~/test"/>
            <property name="hibernate.dialect" value="org.hibernate.dialect.H2Dialect"/> <!-- H2 데이터베이스 사용 -->

            <!-- 옵션 -->
            <property name="hibernate.show_sql" value="true"/>
            <property name="hibernate.format_sql" value="true"/>
            <property name="hibernate.use_sql_comments" value="true"/>
            <!--<property name="hibernate.hbm2ddl.auto" value="create" />-->
        </properties>
        </persistence-unit>

</persistence>
```

---

## JPA 구동 방식

###  Persistence
- `META-INF/persistence.xml` 파일에서 설정 정보 조회
- `EntityManagerFactory` 생성
- `EntityManager`를 가져와서 사용


```java
EntityManagerFactory emf = Persistence.createEntityManagerFactory("hello");
EntityManager em = emf.createEntityManager();
EntityTransaction tx = em.getTransaction();
tx.begin();                         // 트랜잭션 시작

try{
    Member member = new Member();   // 엔티티 생성

    member.setId(1L);
    member.setName("test");

    em.persist(member);             // 영속성 컨텍스트에 저장 -> insert 쿼리 생성

    tx.commit();                    // 트랜잭션 종료 -> DB에 반영
} catch (Exception e) {
    tx.rollback();
} finally {
    em.close();
}

emf.close();
```
> `createEntityManagerFactory`에 넘겨주는 String 값은 persistence.xml 파일의 `persistence-unit`의 name 값

- `EntityManageFactory`는 하나만 생성해서 애플리케이션에서 전체 공유로 사용
- `EntityManager`는 쓰레드간 공유 x
- 데이터 변경은 항상 트랜잭션 안에서 실행, 단순한 데이터 조회는 제외


---

## CRUD

- `em.persist(member)` : 테이블에 해당 member의 PK가 있으면 update, 없으면 insert
- `em.remove(member)` : delete
- `em.find(Member.class, 1L)` : select

### JPQL
- 객체지향쿼리
- 엔티티 객체를 대상으로 쿼리 -> DB에 종속되지 않음 (페이징 처리 등등)

```java
List<Member> result = em.createQuery("select m from Member m", Member.class)
                        .setfirstResult(1)
                        .setMaxResults(10)
                        .getResultList();
```

---

## Mapping

### @Entity
- `@Entity` 사용시 클래스명과 동일한 테이블 생성
- JPA 사용해서 테이블에 매핑할 클래스 관리
- 기본 생성자 필수

### @Table
- 엔티티와 매핑할 데이터베이스 테이블 지정
- 엔티티명과 다른 테이블이름 사용시에는 `@Table(name="")` 사용

## 데이터베이스 스키마 자동 생성
- 애플리케이션 실행 시점에 DDL 자동 생성

### hibernate.hbm2ddl.auto
- `create` : 실행시점 drop + create
- `create-drop` : 실행시 create + 종료시 drop
- `update` : 변경분만 반영, delete는 안됨
- `validate` : 엔티티와 테이블 매핑 확인
- `none`

> `create`, `create-drop`, `update` 3개는 개발환경에서만 사용하기, 운영환경에서 사용 x

### @Column
- DDL 생성시에만 관여, JPA 실행에는 상관 x
- 컬럼 관련 제약조건 추가
- @Column 속성
    - `name`
    - `nullable`
    - `insertable`
    - `updatable`
    - `length`
    - `unique` : 이름이 자동생성이기 때문에 @Table(uniqueConstraints="") 사용 권장
    - `precision, scale`: `BigDecimal` 타입일 때 `precision=19, scale=2` 와 같이 설정 가능


### @Enumerated
- Enum 타입 매핑
- `@Enumerated(Enumtype.STRING)`
    - EnumType은 항상 STRING으로 사용하기
    - ORDINAL 사용시 중간에 enum 추가시 순서로 저장하기 때문에 꼬일 수 있음

### @Temporal
- 날짜 타입 매핑
- TemporalType : `DATE`, `TIME`, `TIMESTAMP`
- (java8) `LocalDate`, `LocalDateTime` 사용시 생략 가능

### @Lob
- BLOB, CLOB 매핑 - Binary 타입
- 매핑하는 필드 타입이 문자면 CLOB 매핑, 나머지는 BLOB 매핑

### @Transient
- 메모리상에서만 관리, 필드 매핑 x


---

## 기본키 매핑

### 직접 사용
- `@Id` 사용

### 자동 생성
- **`@GenertedValue(strategy = GenerationType.IDENTITY)`**
- `IDENTITY` : 데이터베이스에 기본 키 생성 위임
    - • IDENTITY 전략은 em.persist() 시점에 즉시 INSERT SQL 실행하고 DB에서 식별자를 조회
- `SEQUENCE` : 유일한 값을 순서대로 생성하는 특별한 데이터베이스 오브젝트(예: 오라클 시퀀스)
    - 시퀀스 이름 지정
    ```java
    @Entity
    @SequenceGenerator(name="MEMBER_GEN_SEQUENCE", sequenceName = "MEMBER_SEQ", initialValue = 1, allocationSize = 1)
    public class Member {
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "MEMBER_SEQ_GENERATOR")
        private Long id;
    }
    ```
- `TABLE` : 키 생성 전용 테이블 생성해서 사용, 성능이 단점

### JPA Naming 자동변환
- SpringBoot default : `org.springframework.boot.orm.jpa.SpringNamingStrategy`
    - Java Field Name : camelCase
    - Database Column Name : under_score
    - Java : `memberId` => DB : `member_id` (자동변환)

---

# JPA 연관관계 매핑

- 객체지향적으로 매핑하기
- Table은 외래키로 조인 - 객체로 본다면 `field`
- 객체는 참조를 사용 - (객체 참조 = `Class` 참조)

=> 따라서
- JPA에서의 매핑은 외래키 기준(=특정 `Field`)이 아닌
- 객체(=`Class`)를 참조해서 사용

```java
String teamId   (x) - 외래키(단일 필드) 참조 x
Team team       (o) - 객체 참조 o
```

## 외래키 매핑
- **연관관계의 주인은 `FK`가 있는곳 : `@JoinColumn`이 있는 곳**
- 연관관계의 주인쪽에서 데이터 관리(등록, 수정)
- 양방향 연관관계 시 주인이 아닌 쪽은 `mappedBy` 속성을 통해 연관관계 주인을  조인하기
- 주인이 아닌쪽은 데이터 읽기만 가능
- `@OneToMany`와 같이 컬렉션 필드는 `new ArrayList<>()` 와 같이 초기화 해주기(관례)

### 양방향 매핑시 실수할 수 있는 부분
- 연관관계의 주인에 값 입력하기

```java
    Team team = new Team();
    team.setName("TeamA");
    em.persist(team);

    Member member = new Member();
    member.setName("member1");

    team.getMembers().add(member);  // 연관관계의 주인이 아닌쪽에만 값 세팅하면 오류 발생
    member.setTeam(team);           // 연관관계의 주인에 값 입력해야함!

    em.persist(member);
```
- 객체 관점에서 생각하면 양방향 연관관계에 값 세팅시 항상 양쪽에 값을 입력해야 한다
- 연관관계 값 세팅시 편의 메소드를 생성해주자 -> 양쪽말고 한쪽에만 생성을 권장(무한루프 주의)
```java
public class Member {
    ...
    @ManyToOne
    @JoinColumn("team_id")
    private Team team;

    public void setTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }
}
```
- 양방향 참조시 무한루프 돌 수 있으므로 주의
    - ex) `toString(), lombok, JSON 생성 라이브러리`
- `Controller` 에서 `Entity` 말고 `DTO`로 변환해서 반환하기를 권장
    - 엔티티 무한루프 가능성, 엔티티 스펙이 변경되면 api 스펙이 변경될 수 있음

### 양방향 매핑 정리
- **단방향 매핑으로만으로 연관관계 매핑 완료**
- 양방향 매핑은 필요할때만 추가해주기
- 기본적으로 단방향 매핑(외래키 있는 곳을 연관관계 주인으로 설정 - `@JoinColumn`)만 설정
    - 필요할때 양방향 관계(`mappedBy`) 설정해주기(테이블에 영향 x, 조회만 추가됨)
- DB 기준으로 봐도 FK가 있는곳에서 쿼리를 작성함 -> 연관관계에서 주인이 있는곳을 기준으로 조회함 (만약 반대의 경우가 생긴다면 설계 다시 확인해보기, 비즈니스 로직상 필요한지 고려해보기)
    - ex) `Order`-`OrederItem` 연관관계에서 `Order`의 `orderItems` 참조 등


---

## 다양한 연관관계 매핑
- 일대일 : `@OneToOne`
- 일대다 : `@OneToMany`
- 다대일 : `@ManyToOne`
- 다대다 : `@ManyToMany`

### 테이블과 객체의 관점 차이
- 테이블 : 외래키로 조인함, 방향이 없음
- 객체 : 참조용 필드를 통해서 참조 가능
    - 한쪽만 참조(단방향) / 양쪽이 서로 참조(양방향)
    - 양방향 관계일시 참조가 2군데이므로 외래키를 관리할 곳(연관관계 주인) 지정 필요

### 다대일
- `@ManyToOne`
- 제일 기본적인 연관관계 매핑
- 외래키를 사용하는 테이블 -> 해당 엔티티에 참조할 필드 추가
- 양방향으로 참조시 외래키 있는쪽이 주인

    |MEMBER(TABLE)|Member(Entity)|
    ---|---|
    |MEMBER_ID(PK)|memberId|
    |TEAM_ID(FK)|Team team|
    |USERNAME|username|


### 일대다
- `@OneToMany`
- **단방향**으로 매핑할 때는 `@JoinColumn` 반드시 지정해주기
    - `@JoinColumn` 추가 안하면 조인 테이블 생성해서 사용함
    - ex) `Member - Team` 이면 `Team` 엔티티에 아래처럼 추가
    ``` java
    @OneToMany
    @JoinColumn(name = "team_id") // Member 테이블의 외래키 컬럼명
    List<Member> members
    ```
    - 만약 insert를 하면 외래키가 다른 테이블(참조하는 엔티티)에 있기 때문에 데이터 변경시 추가로 `UPDATE` sql을 실행함 (insert 후 외래키쪽을 update 해서 관계 매핑)
    - 일대다 단방향 매핑보다는 **다대일 양방향 매핑을 사용**하자
- **양방향**으로는 공식적으로 존재 x
    - 한다면 둘 다 `@JoinColumn`을 사용후에 외래키가 있는 엔티티에 `@JoinColumn(insertable=false, updatable=false)`을 통해서 읽기 전용 필드로써 사용하기
    - 일대다 양방향은 사용 x, 다대일 양방향 매핑으로 사용하기

### 일대일
- 외래키에 유니크 제약을 추가해서 사용
- 주 테이블에 외래키 단방향 -> `@ManyToOne` 단방향 매핑과 유사
- 주 테이블에 외래키 양방향 -> 외래키가 있는 곳(유니크 제약조건 있는곳)을 주인으로 하고 반대쪽을 `mappedBy` 적용시키기

### 다대다
- 관계형 데이터베이스는 정규화된 테이블 2개로 다대다 관계 표현 x, 객체는 컬렉션을 통해서 다대다 매핑 가능 -> 단순히 연결만 될 수 있음
- 연결 테이블 추가해서 일대다, 다대일 관계로 풀어내야 함
- `@JoinTable` 사용하면 다대다 관계 매핑시 자동으로 연결 테이블 생성해줌
- 실무에서는 사용 x, 연결 테이블용 엔티티 추가하기

---
### @JoinColumn
- name : 매핑할 외래키 이름
- referenencedColumnName : 외래키가 참조하는 대상 테이블의 컬럼명, defalut는 참조하는 테이블의 기본키 컬럼명 (외래키 이름이 다를 때 사용)

### @ManyToOne
- optional : false 지정 시 연관된 엔티티가 null이 아니어야함, default TRUE
- fetch : default FetchType.EAGER
- cascade : 영속성 전이 기능

### @OneToMany
- mappedBy : 연관관계 주인 지정
- fetch : default FetchType.LAZY
- cascade

---

## 상속관계 매핑

- 관련 어노테이션
- `@Inheritance(strategy = Inheritance.Type.XXX)` : 부모 테이블에 지정
    - `JOINED` : 조인전략
    - `SINGLE_TABLE` : 단일 테이블 전략
    - `TABLE_PER_CLASS` : 구현 클래스마다 테이블 전략

- Single table 사용시 지정
    - `@DiscriminatorColumn(name="DTYPE")`
        - name에는 다른 테이블들과 구별하는 컬럼명을 지정
    - `@DiscriminatorValue("XXX")`
        - DiscriminatorColumn에 들어갈 값 지정

### 조인전략
- 객체관점에서 제일 정석
- 장점
    - 테이블 정규화, 저장공간 효율화, 외래키 참조 무결성 제약조건 활용가능
- 단점
    - 조회시 조인 -> 성능 저하, 쿼리 복잡

### 단일 테이블 전략
- 장점
    - 조인 필요 x -> 조회 성능 빠름, 조회 쿼리 단순
- 단점
    - 자식 엔티티가 매핑하는 컬럼은 모두 null 허용 -> type값으로 구분하고 나머지 값들은 다 null로 넣어줌
    - 단일테이블에 모두 저장 -> 테이블이 커진다 -> 조회 성능이 느려질 수 있음

### 구현 클래스마다 테이블 전략
- 객체 관점에서도 DB 관점에서도 추천 x -> 사용하지 말 것.
- 클래스마다 테이블 생성하기 때문에 여러 자식 테이블 함께 조회하려면 성능이 느림
- 컬럼이 변경시 사이드이펙트가 큼

```
 - 테이블 구조가 매우 단순하고 확장가능성이 없으면 단일 테이블 전략
 - 비즈니스적으로 중요하고 복잡하면 조인 전략
```

## @MappedSuperclass
- 공통 매핑 정보(createdDate, createdBy, ...)
- 엔티티 x, 테이블과 매핑x -> 조회, 검색 불가
- 부모 클래스를 상속받는 자식 클래스에 매핑 정보 제공
- 추상 클래스 권장
- 테이블과 관련 x, 단순히 엔티티가 공통으로 사용하는 매핑 정보를 모으는 용도
    - ex) 등록자, 등록일, 수정자, 수정일 등...
```java
@MappedSuperclass
public abstract class BaseEntity {
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifedBy;
    private LocalDateTime modifiedDate;
}
```

---

# 프록시
- `em.find` vs `em.getReference()`
- `em.getReference()` : 데이터베이스 조회를 미루는 가짜(프록시) 엔티티 객체 조회
- 해당 메소드 호출시에는 조회 x, 값을 사용할 때 DB에서 조회

```java
Member findMember = em.getReference(Member.class, "id1");   // 프록시 객체 조회, 실제 DB 조회 x
system.out.println(findMember.getName());               // 프록시 객체가 실제 객체 조회(이때 DB에서 조회)
```
### 프록시 객체의 초기화
    MemberProxy 객체의 Member target 조회
    1. MemberProxy 객체의 getName() 메소드 호출
    2. 영속성 컨텍스트에 초기화 요청
    3. DB 조회
    4. 실제 Entity 생성
    5. Member Entity 조회
    6. target.getName() 호출

- 프록시 특징
    - 실제 클래스 상속받아서 생성됨
    - 프록시 객체는 실제 객체의 참조를 보관
    - 프록시 객체 호출 -> 프록시 객체는 실제 객체의 메소드 호출
    - 프록시 객체는 처음 사용할 때 한 번만 초기화
    - 프록시 객체를 초기화 할때 실제 엔티티로 바뀌는 것이 아님
    - 초기화 되었을 때 프록시 객체를 통해 실제 엔티티에 접근이 가능해지는 것
    - 프록시 객체는 원본 엔티티 상속 -> 타입 체크 주의
        - **`==` 비교 대신 `instance of` 사용**
        ```java
        Member m1 = em.find(Member.class, "id1");
        Member m2 = em.find(Member.class, "id2");

        m1.getClass() == m2.getClass() // true

        Member m1 = em.find(Member.class, "id1");
        Member m2 = em.getReference(Member.class, "id2");

        m1.getClass() == m2.getClass() // false

        m1 instance of Member   // true
        m2 instance of Member   // true
        ```
    - 영속성 컨텍스트에 찾는 엔티티가 이미 존재 -> `em.getReference()` 호출해도 실제 엔티티 반환
        ```java
        Member m1 = em.find(Member.class, "id1");   // 영속성 컨텍스트에 Member 1차 캐시 넣어줌
        System.out.println(m1.getClass()); // Member

        Member reference = em.getReference(Member.class, member1.getId());  // 영속성 컨텍스트에 이미 존재하므로 proxy 객체가 아닌 실제 객체 반환
        System.out.println(reference.getClass()); // Member, Not Proxy

        System.out.println(m1 == reference); // true
        ```
        - **영속성 컨텍스트에서 동일한 엔티티라면 한 트랜잭션 안에서 항상 동일함을 보장해줌**
        - 실제 엔티티 객체 조회 -> em.getReference 호출해도 실제 엔티티 반환
        - proxy 객체 조회 -> em.find 호출해도 proxy 객체 반환

    - 영속성 컨텍스트의 도움을 받을 수 없는 준영속 상태일 때, 프록시를 초기화하면 문제 발생
    ```java
    try {

        Member refMember = em.getReference(Member.class, "id1");    // Proxy객체 호출

        em.detach(refMember);   // 영속성 컨텍스트에서 관리 해제

        refMember.getName();    // Proxy 객체가 실제 엔티티 호출하려고 하면 Exception 발생 : initialization Proxy

        tx.commit();
    } catch(Exception e) {
        e.printStackTrace();
        tx.rollback();
    }
    ```

    - 프록시 인스턴스 초기화 여부 확인
    ```java
    // 엔티티 매니저 팩토리
    emf.getPersistenceUnitUtil.isLoaded(Object entity)
    ```

    - Hibernate 프록시 강제 초기화
    ```java
    Hibernate.initialize(Object entity);
    ```

    - JPA 표준은 강제 초기화 없음
    - 강제 호출 : `refMember.getName()`

## 즉시로딩과 지연로딩
- 지연로딩(LAZY)
    - 프록시 객체를 가져옴
    - 실제 값 사용할 때 조회해옴
- 프록시와 즉시로딩 주의
    - **최대한 지연 로딩만 사용**
    - 즉시로딩은 JPQL에서 N+1 문제를 일으킴
        ```java
        List<Member> list = em.createQuery("select m from Member m", Member.class).getResultList()
        ```
        - 해당 쿼리는 sql로 `select * from member`으로 번역됨
        - 즉시로딩이기 때문에 데이터를 가져올때 team 정보도 들어있어야함
        - `select * from team` 쿼리로 team 정보를 가져옴 -> n+1 문제 발생
- 즉시로딩은 쿼리가 어떻게 날아갈 지 예측할 수 없음 -> 실무에서 즉시로딩 지양
- `@xxxToOne`은 기본이 즉시로딩, `@xxxToMany`는 기본이 지연로딩

### 영속성 전이: CASCADE
- 특정 엔티티를 영속 상태로 만들 때 연관된 엔티티도 함께 영속 상태로 변경
- 부모 엔티티 저장할 때 자식 엔티티도 함께 저장
```
@OneToMany(mappedBy="parent", cascade=CascaseType.ALL)
```
- 영속성 전이는 연관관계 매핑과 관련 x
- cascade 종류
    - `ALL` 전부
    - `PERSIST` 저장
    - `REMOVE`  삭제
    - `MERGE` 업데이트
- 자식 엔티티와 부모 엔티티만 연관관계가 있을 때/부모 엔티티만 자식 엔티티를 소유할 때 사용
- 만약 자식 엔티티가 다른 엔티티와도 관계가 있을때는 사용 x

### 고아 객체
- 부모 엔티티와 연관관계가 끊어진 자식 엔티티 자동으로 삭제
- `orphanRemoval = true`
```java
Parent parent1 = em.find(Parent.class, id);
parent1.getChildren().remove(0); // Delete 쿼리 발생
```
- 참조하는 곳이 하나일 때 사용
- 특정 엔티티가 개인 소유할 때 사용
- `CascadeType.REMOVE`처럼 동작
    - 개념적으로 부모 제거 -> 자식 고아 -> 자식도 함께 제거됨
    - 자식 먼저 전부 제거하는 쿼리가 n개 날라간 후 부모 제거 쿼리 날아감

> 영속성 전이와 고아 객체 모두 부모에서 자식 엔티티의 생명주기 관리할 때 사용 -> `OneToXXX`에서 사용


### 값 타입
- 기본값 타입
    - ex) `String name`, `int age`
    - 생명주기를 엔티티에 의존
    - 기본값 타입은 공유 x, 항상 값만 복사함
    - `Integer` 같은 래퍼 클래스나 `String` 같은 클래스는 공유는 가능하지만 변경 x
    - -> 사이드 이펙트 발생 x
- 임베디드 타입
    - 기본 값 타입을 모아 직접 정의한 값 타입
    - `@Embeddable` : 값 타입 정의하는 곳
    - `@Embedded` : 값 타입 사용하는 곳
    - 두 어노테이션 중 하나만 사용해도 임베디드 값 타입으로 사용 가능함
    - 기본 생성자 필수
    - 임베티드 타입은 엔티티의 값일 뿐 테이블에서는 임베디드 값을 사용하기 전이나 후나 동일함 -> 매핑하는 테이블은 기본 값으로 들어감
    - 객체와 테이블을 아주 세밀하게 매핑 가능 -> 객체지향적인 설계가 가능함
    - **Period**
        ```java
        @Embeddable
        public class Period {
            private Date startDate;
            private Date endDate;
        }
        ```
    - **Member**
        ```java
        @Entity
        public class Member {
            ...
            @Embedded
            private Period workPeriod;
        }
        ```
    - 한 엔티티에서 같은 값 타입 사용 -> 컬럼명 중복됨 -> 컬럼명 속성 재정의
        - `@AttributeOverrides({name="startDate", value=@AttributeOverride(name="workStartDate")})`
    - 임베디드 타입은 객체이므로 여러 엔티티에서 공유하면 위험함 -> 값만 복사해서 사용하기
    - 객체의 공유 참조는 피할 수 없음
    ```java
    Address address = new Address("city", "street", "zipcode");
    member1.setAddress(address);
    member2.setAddress(address);

    member1.getAddress().setCity("newCity"); // member1, member2 둘 다 update 쿼리 발생
    ```
    - **값 타입은 불변 객체로 설계**해야함, 생성 이후로 값 변경 x
        - 생성자로만 값 설정하고 Setter 생성 x
        - 값 타입의 동등성을 보장하기 위해 `equals()`와 `hashcode()`를 재정의 필요 + 정의할때 `get`메소드 활용하는게 안전(프록시 객체 때문에)
     ```java
    Address address = new Address("city", "street", "zipcode");
    member.setAddress(address);
    Address newAddress = new Address("city", address.getStreet(), address.getZipcode());
    member.getAddress(newAddress); // 값 변경시 불변객체를 통해 새로 세팅하기
    ```

    - 값 타입의 비교
    ```
    - 기본값 타입은 동일성 비교 : 인스턴스 참조 값 비교, == 사용
    - 객체타입은 동등성 비교 : 인스턴스의 값 비교, equals() 사용
        - a.equals(b)를 사용해서 동등성 비교하기 -> equals() 메소드 적절하게 재정의 필요
    ```

- 값 타입 컬렉션
    - 값 타입을 하나 이상 지정시 사용
    - `@ElementCollection, @ColleactionTable`
    - DB는 컬렉션 값 저장 x -> 별도의 테이블로 매핑
    - 값 타입 컬렉션도 결국 값 타입 -> 생명주기는 엔티티가 관리
        - 영속성 전이 + 고아 객체 제거 기능
    - 값 타입 컬렉션도 결국 컬렉션 -> 지연로딩으로 조회
    - 값 타입 컬렉션의 변경사항 발생 -> 부모 엔티티와 연결된 값 타입 컬렉션 전체 삭제 후 현재 들어있는 값 다시 전부 insert
    - 실무에서는 정말 간단한 경우 외에는 값 타입 컬렉션 사용보다는 일대다 관계로 대신하기
        - 영속성 전이와 고아 객체 제거를 사용해 값 타입 컬렉션처럼 사용


|엔티티 타입|값 타입|
|---|---|
|식별자O|식별자X|
|생명주기 관리|생명주기 엔티티에 의존|
|공유O|공유하지 않는 것이 안전|

---

# JPQL
- 객체지향SQL
- 동적쿼리 작성이 어려움

### 반환 타입
- `TypeQuery` : 반환 타입 명확
    ```java
    TypedQuery<Member> query = em.createQuery("SELECT m FROM Member m", Member.class);
    ```

- `Query` : 반환 타입이 불명확
    ```java
    Query<Member> query = em.createQuery("SELECT m.username, m.age FROM Member m", Member.class);
    ```

### 결과 조회 API
- `getResultList()` : 결과 하나 이상, 없으면 빈 리스트
- `getSingleResult()` : 결과 하나, 없거나 둘 이상이면 Exception
    - Spring Data JPA는 결과 없으면 Optional 반환

### 파라미터 바인딩
- 이름 기준
    ```java
    em.createQuery("SELECT m FROM Member m where m.username=:username").setParameter("username", usernameParam);
    ```
- 위치 기준
    ```java
    em.createQuery("SELECT m FROM Member m where m.username=?1").setParameter(1, usernameParam);
    ```

### 프로젝션
- SELECT 절에 조회할 대상 지정
- 여러 값 조회시 : ex) m.username, m.age
    1. Query 타입으로 조회
    ```java
    List result = em.createQuery("SELECT m.username, m.age FROM Member m").getResultList();
    Object o = result.get(0);
    Object[] member = (Object[]) o;
    System.out.println("username="+member[0]);
    System.out.println("age="+member[1]);
    ```
    2. Object[]로 조회
    ```java
    List<Object[]> result = em.createQuery("SELECT m.username, m.age FROM Member m").getResultList();
    ```
    3. DTO로 조회
    ```java
    List<MemberDTO> result = em.createQuery("SELECT new dto.MemberDTO(m.username, m.age) FROM Member m", MemberDTO.class).getResultList();
    ```
    - 순서와 타입 일치하는 생성자 필요
    - new 키워드와 package 경로를 포함한 클래스 전체 이름 입력 필요

### 페이징 API
- `setFirstResult(int offset)` : 조회 시작 위치(0부터 시작)
- `setMaxResult(int limit)` : 가져올 데이터 개수
- `hibernate.dialect`로 설정한 데이터베이스에 맞게 페이징 쿼리 날아감

### 조인
- 내부조인
- 외부조인
- 세타조인 : 카타시안 곱으로 조회, 연관관계 없을 때 조인할 경우
- ON절을 활용한 조인
    - 조인 대상 필터링
        - ex) 회원과 팀을 조인하면서, 팀 이름이 A인 팀만 조인
        - **JPQL**
            ```SQL
            SELECT m,t FROM Member m
            LEFT JOIN m.team t on t.name = 'A'
            ```
        - **SQL**
            ```SQL
            SELECT m.*, t.* FROM Member m
            LEFT JOIN Team t ON m.team_id = t.id and t.name = 'A'
            ```
    - 연관관계 없는 엔티티의 외부 조인
        - ex) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
        - **JPQL**
            ```SQL
            SELECT m, t FROM
            Member m LEFT JOIN Team t on m.username = t.name
            ```
        - **SQL**
            ```SQL
            SELECT m.*, t.* FROM
            Member m LEFT JOIN Team t ON m.username = t.name
            ```

### 서브쿼리
- 지원함수
    - 서브 쿼리 지원 함수
        - [NOT] EXISTS (subquery): 서브쿼리에 결과가 존재하면 참
        - {ALL | ANY | SOME} (subquery)
            - ALL 모두 만족하면 참
            - ANY, SOME: 같은 의미, 조건을 하나라도 만족하면 참
        - [NOT] IN (subquery):
    - 예시)
        - 팀A 소속인 회원
        ```SQL
        select m from Member m
        where exists (select t from m.team t where t.name = ‘팀A')
        ```
        - 전체 상품 각각의 재고보다 주문량이 많은 주문들
        ```SQL
        select o from Order o
        where o.orderAmount > ALL (select p.stockAmount from Product p)
        ```
        - 어떤 팀이든 팀에 소속된 회원
        ```SQL
        select m from Member m
        where m.team = ANY (select t from Team t)
        ```
- 한계
    - JPA는 WHERE, HAVING 절에서만 서브 쿼리 사용 가능
    - SELECT 절도 가능(하이버네이트에서 지원, 표준 스펙은 아니지만 대부분 사용 가능)
    - **FROM 절의 서브 쿼리는 현재 JPQL에서 불가능**
        - 조인으로 풀 수 있으면 풀어서 해결


### Criteria
- JPA 표준 스펙
- 동적 쿼리 생성이 쉬움
- 컴파일 에러가 가능함
- 복잡하기 때문에 실무에서는 활용 x

























