import domain.Address;
import domain.Member;
import domain.Team;
import domain.item.Album;
import domain.item.Book;
import domain.item.Item;
import domain.item.Movie;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JpaTests {

    static EntityManagerFactory emf;   // EntityManagerFactory 는 Thread-safe -> 하나만 생성해서 애플리케이션 전체에서 공유
    EntityManager em;                  // EntityManger 는 Thread 간 공유 X -> 요청 들어올때 생성해서 사용하고 close 해주어야함
    EntityTransaction tx;

    @BeforeAll
    @DisplayName("Persistence.createEntityManagerFactory(String persistenceUnitName) 에서" +
            "persistenceUnitName 은 persistence.xml 에서 설정한 persistence-unit 의 name 값")
    public static void init_first() {
        System.out.println("createEntityManagerFactory");
        emf = Persistence.createEntityManagerFactory("hello");
    }

    @BeforeEach
    public void init() {
        em = emf.createEntityManager();
        tx = em.getTransaction();
        System.out.println("transaction start -----------------------------------------");
        tx.begin();
    }

    @AfterEach
    public void finish() {
        tx.commit();
        System.out.println("transaction close -----------------------------------------");
        em.close();
    }

    @AfterAll()
    public static void finish_last() {
        System.out.println("EntityManagerFactory close");
        emf.close();
    }

    @Test
    @DisplayName("영속성 컨텍스트에 1차캐시로 올라가있기 때문에 DB에 다녀오지 않는다. " +
            "-> select 쿼리 안나감")
    public void 영속성컨텍스트_1차캐시_조회() {
        Member member = new Member();
        member.setName("test");

        em.persist(member);

        Member findMember = em.find(Member.class, member.getMemberId());
        assertEquals(member, findMember);
    }

    @Test
    public void DB조회() {
        Member member = new Member();
        member.setName("test");

        em.persist(member);
        em.flush();
        em.clear();

        Member findMember = em.find(Member.class, member.getMemberId());
        assertEquals(member.getMemberId(), findMember.getMemberId());
    }

    @Test
    @DisplayName("프록시 객체는 실제 객체의 참조를 보관하고 있다가 " +
            "값에 접근하면 초기화를 통해 실제 객체의 메소드를 호출한다. ")
    public void 프록시_객체_조회() {
        Member member = new Member();
        member.setName("test");

        em.persist(member);
        em.flush();
        em.clear();

        Member refMember = em.getReference(Member.class, member.getMemberId()); // 프록시 객체 조회
        System.out.println("refMember = " + refMember.getClass().getName());

        assertEquals(member.getName(), refMember.getName());    // getName() 메소드를 사용할 때 프록시 객체 초기화
    }

    @Test
    @DisplayName("준영속 상태(detach, remove)의 프록시 객체를 초기화 하려고 하면 Exception 발생")
    public void 준영속_상태의_프록시_객체_조회() {
        Member member = new Member();
        member.setName("test");
        em.persist(member);

        em.flush();
        em.clear();

        Member refMember = em.getReference(Member.class, member.getMemberId());    // Proxy객체 호출
        em.detach(refMember);   // 영속성 컨텍스트에서 관리 해제

        assertThrows(LazyInitializationException.class, refMember::getName); // Proxy 객체가 실제 엔티티 호출하려고 하면 Exception 발생 : initialization Proxy
    }

    @Test
    @DisplayName("영속성 컨텍스트에서 동일한 엔티티는 항상 동일함을 보장해주기 때문에 " +
            "프록시 객체를 먼저 조회하면 이후로 실제 엔티티를 조회해도 프록시 객체가 조회되고 " +
            "그 반대의 경우도 동일하다.")
    public void 영속성_컨텍스트의_동일성_보장() {
        Member member = new Member();
        member.setName("test");

        em.persist(member);
        em.flush();
        em.clear();

        Member refMember = em.getReference(Member.class, member.getMemberId());     // 프록시 객체 먼저 조회
        Member findMember = em.find(Member.class, member.getMemberId());            // 이후 엔티티 조회

        // 둘 다 프록시 객체로 조회됨
        System.out.println("refMember.getClass().getName() = " + refMember.getClass().getName());
        System.out.println("findMember.getClass().getName() = " + findMember.getClass().getName());

        assertEquals(refMember, findMember);

        em.flush();
        em.clear();

        Member findMember2 = em.find(Member.class, member.getMemberId());           // 엔티티 먼저 조회
        Member refMember2 = em.getReference(Member.class, member.getMemberId());    // 이후 프록시 객체 조회

        // 둘 다 엔티티 객체로 조회됨
        System.out.println("findMember2.getClass().getName() = " + findMember2.getClass().getName());
        System.out.println("refMember2.getClass().getName() = " + refMember2.getClass().getName());

        assertEquals(findMember2, refMember2);
    }

    @Test
    @DisplayName("@Inheritance(strategy = InheritanceType.xxx)" +
            "JOINED: 조인전략" +
            "SINGLE_TABLE: 단일 테이블 전략" +
            "TABLE_PER_CLASS: 구현 클래스마다 테이블 전략")
    public void 상속관계_매핑() {

        Album album = new Album();
        album.setArtist("artist");
        album.setName("album_name");
        album.setPrice(30000);
        em.persist(album);

        Book book = new Book();
        book.setAuthor("author");
        book.setIsbn("00000000");
        book.setName("book_name");
        book.setPrice(18000);
        em.persist(book);

        Movie movie = new Movie();
        movie.setDirector("director");
        movie.setActor("actor");
        movie.setName("movie_name");
        movie.setPrice(8000);
        em.persist(movie);
    }

    @Test
    @DisplayName("@MappedSuperclass" +
            "자식 클래스에 매핑 정보만 제공, 공통으로 사용하는 속성 모을 때 사용" +
            "직접 생성해서 사용할 일 x -> 추상 클래스 권장")
    public void 공통속성_매핑() {
        Member member = new Member();
        member.setName("test");

        // BaseEntity 를 통해 공통속성을 따로 모아서 정의 할 수 있음
        member.setCreatedBy("user");
        member.setCreatedTime(LocalDateTime.now());
        member.setModifiedBy("user");
        member.setModifiedTime(LocalDateTime.now());

        em.persist(member);
    }

    @Test
    public void 값_타입_컬렉션은_지연로딩과_영속성_전이를_합친_것과_같다() {
        domain.Member member = new domain.Member();
        member.setName("test");
        // 컬렉션에 추가하면 DB에 insert 쿼리 날아감
        member.getFavoriteFoods().add("치킨");
        member.getFavoriteFoods().add("피자");
        member.getFavoriteFoods().add("족발");

        em.persist(member);

        em.flush();
        em.clear();

        domain.Member findMember = em.find(domain.Member.class, member.getMemberId());

        Set<String> favoriteFoods = findMember.getFavoriteFoods();
        for (String favoriteFood : favoriteFoods) {
            System.out.println("favoriteFood = " + favoriteFood);
        }

        // 치킨 -> 한식
        // 컬렉션을 변경하면 DB에 쿼리 날아감
        favoriteFoods.remove("치킨");
        favoriteFoods.add("한식");
    }

    @Test
    @DisplayName("값 타입 컬렉션에 변경사항이 발생하면 주인 엔티티와 연관된 데이터 전부 삭제 후 " +
            "값 타입 컬렉션의 현재 값을 다시 전부 저장한다." +
            "그러므로 사용하지 말자.")
    public void 값_타입_컬렉션_데이터_변경() {
        Member member = new Member();
        member.setName("test");
        member.getAddresses().add(new Address("city", "street"));
        member.getAddresses().add(new Address("oldCity", "oldStreet"));

        em.persist(member);

        em.flush();
        em.clear();

        Member findMember = em.find(Member.class, member.getMemberId());
        // 컬렉션에서 remove 를 할 때 Member 의 모든 Addresses 가 삭제됨 -> delete
        findMember.getAddresses().remove(new Address("oldCity", "oldStreet"));

        // 이후 현재 컬렉션에 들어있는 값들을 다시 insert 해줌
        findMember.getAddresses().add(new Address("newCity", "newStreet"));

        // 따라서 컬렉션에 이미 저장되어 있던 city 와 새로 add 된 newCity 를 insert 함 -> insert 쿼리 2개 날아감
        assertEquals(2, findMember.getAddresses().size());
    }

    @Test
    public void criteria() {
        Member member = new Member();
        member.setName("test");
        em.persist(member);

        // Criteria 사용 준비
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Member> query = cb.createQuery(Member.class);

        // 루트 클래스 (조회할 클래스)
        Root<Member> m = query.from(Member.class);

        // 쿼리 생성
        CriteriaQuery<Member> cq = query.select(m).where(cb.equal(m.get("name"), "test"));
        List<Member> members = em.createQuery(cq).getResultList();

        System.out.println("member = " + members.get(0));
    }

    @Test
    public void 네이티브_SQL() {
        Member member = new Member();
        member.setName("test");
        em.persist(member);

        List<Member> members = em.createNativeQuery("SELECT * FROM member WHERE name = 'test'", Member.class).getResultList();
        System.out.println("member = " + members.get(0));
    }

    @Test
    public void JPQL_조인() {
        Team team = new Team();
        team.setTeamName("team");
        em.persist(team);
        
        Member member = new Member();
        member.setName("test");
        member.setTeam(team);
        em.persist(member);
        
        // 내부 조인 = inner join
        Member innerJoin = em.createQuery("select m from Member m join m.team", Member.class).getSingleResult();
        System.out.println("innerJoin = " + innerJoin);
        
        // 외부 조인 = outer join
        Member outerJoin = em.createQuery("select m from Member m left join m.team", Member.class).getSingleResult();
        System.out.println("outerJoin = " + outerJoin);

        // 세타 조인 -> cross join
        long thetaJoin = em.createQuery("select count(m) from Member m, Team t", Long.class).getSingleResult();
        System.out.println("thetaJoin = " + thetaJoin);
    }

    @Test
    public void 사용자_정의_함수() {
        Member member1 = new Member();
        member1.setName("member1");
        em.persist(member1);

        Member member2 = new Member();
        member2.setName("member2");
        em.persist(member2);

        String query = "select group_concat(m.name) from Member m";
        //"select function('group_concat', m.name) from Member m";

        List<String> result = em.createQuery(query, String.class).getResultList();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    @DisplayName("경로표현식에는 상태필드, 단일 값 연관 필드, 컬렉션 값 연관 필드가 있다." +
            "조인을 사용할 때에는 가급적 명시적 조인을 사용하자.")
    public void 경로표현식() {
        Team team = new Team();
        team.setTeamName("team");
        em.persist(team);

        Member member1 = new Member();
        member1.setName("member1");
        member1.setTeam(team);
        em.persist(member1);

        Member member2 = new Member();
        member2.setName("member2");
        member2.setTeam(team);
        em.persist(member2);

        // 상태필드: 경로 탐색의 끝, 탐색 x - m.name
        // 단일 값 연관 필드: 묵시적 내부 조인 발생(inner join), 탐색 o -> m.team
        // 컬렉션 값 연관 필드: 묵시적 내부 조인 발생, 탐색 x (From 절에서 명시적 조인을 통해 별칭을 얻으면 별칭을 통해 탐색 가능) -> m.addresses

        List<Member> implicitJoin = em.createQuery("select t.members from Team t").getResultList();  // 묵시적 조인, 내부 조인만 가능함
        for (Member member : implicitJoin) {
            System.out.println("joinedMember = " + member);
        }

        List<Member> explicitJoin = em.createQuery("select m from Member m join m.team", Member.class).getResultList(); // 명시적 조인
        for (Member member : explicitJoin) {
            System.out.println("joinedMember = " + member);
        }

        assertEquals(implicitJoin, explicitJoin);   // 묵시적 조인과 명시적 조인의 내부조인은 동일

        assertThrows(Exception.class, () -> {
            em.createQuery("select t.members.name from Team t").getResultList();    // 컬렉션은 경로 탐색의 끝, 명시적 조인을 통해 별칭을 얻어야 접근 가능
        });

        List<String> collectionExplicitJoin = em.createQuery("select m.name from Team t join t.members m", String.class).getResultList();   // 컬렉션에서 접근하려면 명시적 조인의 별칭으로 접근
        for (String s : collectionExplicitJoin) {
            System.out.println("name = " + s);
        }
    }
    
    @Test
    @DisplayName("패치 조인은 연관된 엔티티나 컬렉션을 SQL 한 번에 함께 조회가 가능하다")
    public void 패치조인() {
        Team_Member_엔티티_세팅();

        List<Member> resultList = em.createQuery("select m from Member m join fetch m.team", Member.class)
                .getResultList();
        for (Member findMember : resultList) {
            System.out.println("findMember = " + findMember);
        }
    }

    @Test
    @DisplayName("컬렉션 패치 조인시 중복된 데이터가 발생할 수 있다. " +
            "JPQL의 DISTINCT는 SQL에 DISTINCT를 추가해주고 애플리케이션에서 엔티티 중복을 제거해준다." +
            "따라서 컬렉션을 패치 조인할 때는 DISTINCT를 사용하자.")
    public void 컬렉션_패치_조인() {
        Team_Member_엔티티_세팅();

        String queryNotIncludeDistinct = "select t from Team t join fetch t.members";
        // 해당 쿼리로 실행하면 teamA 에 member1, member2 가 속해있기 때문에 teamA가 2개로 나온다.

        String query = "select distinct t from Team t join fetch t.members";

        List<Team> resultList = em.createQuery(query, Team.class)
                .getResultList();

        for (Team team : resultList) {
            System.out.println("teamname = " + team.getTeamName() + ", team = " + team);
            for (Member member : team.getMembers()) {
                System.out.println("-> username = " + member.getName() + ", member = " + member);
            }
        }
    }

    @Test
    @DisplayName("컬렉션을 패치 조인하면 페이징 API 사용 불가능" +
            "일대일, 다대일 같은 단일 값 필드는 패치 조인해도 페이징이 가능" +
            "하이버네이트는 경고 로그 남기고 메모리에서 페이징한다 -> 모두 불러오고 메모리에서 페이징하므로 매우 위험!")
    public void 컬렉션_패치_조회_페이징() {
        Team_Member_엔티티_세팅();

        String query = "select distinct t from Team t join fetch t.members";
        List<Team> resultList = em.createQuery(query, Team.class)
                .setFirstResult(0)
                .setMaxResults(1)
                .getResultList();

        //WARN: firstResult/maxResults specified with collection fetch; applying in memory! 로그 찍힘
        // 쿼리 확인하면 limit 없이 전부 가져온 후에 메모리에서 페이징 처리해서 결과는 페이징 처리된 것처럼 보임

        for (Team team : resultList) {
            System.out.println("teamname = " + team.getTeamName() + ", team = " + team);
            for (Member member : team.getMembers()) {
                System.out.println("-> username = " + member.getName() + ", member = " + member);
            }
        }
    }

    @Test
    @DisplayName("TYPE : 조회 대상을 특정 자식으로 한정" +
            "TREAT: 상속 구조에서 부모 타입을 특정 자식 타입으로 다룰 때 사용 - FROM, WHERE, SELECT(하이버네이트 지원) 사용")
    public void JPQL_다형성_쿼리() {
        Book book = new Book();
        book.setAuthor("kim");
        em.persist(book);

        em.createQuery("select i from Item i where type(i) in (Book, Movie)", Item.class)
                .getResultList();

        em.createQuery("select i from Item i where treat(i as Book).author = 'Kim'", Item.class)
                .getResultList();
    }

    @Test
    @DisplayName("엔티티를 직접 조회하는 것은 엔티티 식별자로 조회하는 것과 동일함")
    public void JPQL_엔티티_직접_조회() {
        Member member = new Member();
        member.setName("test");
        em.persist(member);

        Member findEntity = em.createQuery("select m from Member m where m = :member", Member.class)
                .setParameter("member", member)
                .getSingleResult();

        Member findById = em.createQuery("select m from Member m where m.memberId = :memberId", Member.class)
                .setParameter("memberId", member.getMemberId())
                .getSingleResult();

        assertEquals(findEntity, findById);
    }

    @Test
    @DisplayName("벌크 연산은 영속성 컨텍스트를 무시하고 데이터베이스에 직접 쿼리한다. -> 쿼리 한번으로 여러 테이블 로우 변경 가능" +
            "영속성 컨텍스트에 작업을 수행하기 전 벌크 연산을 먼저 실행하거나" +
            "벌크 연산 수행 후 영속성 컨텍스트를 초기화 해주어야 한다. (Spring Data JPA 에서는 @Modifying 어노테이션에 clearAutomatically 가 설정되어 있음)" +
            "UPDATE, DELETE 지원 + INSERT(insert into .. select, 하이버네이트 지원)")
    public void 벌크연산() {
        Member member1 = new Member();
        member1.setName("member1");
        em.persist(member1);

        Member member2 = new Member();
        member2.setName("member2");
        em.persist(member2);

        int resultCount = em.createQuery("update Member m set m.name = :name")
                .setParameter("name", "member")
                .executeUpdate();

        System.out.println("resultCount = " + resultCount);

        em.clear();
        // 영속성 컨텍스트 초기화 하지 않으면
        // 벌크 연산 하기 전에 들어있는 영속성 1차캐시의 값을 가져오기 때문에
        // member 의 name 이 업데이트 되지 않은 결과를 가져온다.

        List<Member> members = em.createQuery("select m from Member m", Member.class)
                .getResultList();
        for (Member member : members) {
            System.out.println("member = " + member);
        }
    }


    private void Team_Member_엔티티_세팅() {
        Team team1 = new Team();
        team1.setTeamName("teamA");
        em.persist(team1);

        Team team2 = new Team();
        team2.setTeamName("teamB");
        em.persist(team2);

        Member member1 = new Member();
        member1.setName("member1");
        member1.setTeam(team1);
        em.persist(member1);

        Member member2 = new Member();
        member2.setName("member2");
        member2.setTeam(team1);
        em.persist(member2);

        Member member3 = new Member();
        member3.setName("member3");
        member3.setTeam(team2);
        em.persist(member3);

        em.flush();
        em.clear();
    }
}
