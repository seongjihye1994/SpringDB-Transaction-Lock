package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import java.sql.SQLException;

import static hello.jdbc.connection.ConnectionConst.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 트랜잭션 - 트랜잭션 탬플릿 사용
 */
class MemberServiceV3_2Test {

    public static final String MEMBER_A = "memberA";
    public static final String MEMBER_B = "memberB";
    public static final String MEMBER_EX = "ex";

    private MemberRepositoryV3 memberRepository;
    private MemberServiceV3_2 memberService;

    // 각 테스트 시작 전 수행되는 메소드!! - 테스트 전 초기 세팅
    @BeforeEach
    void before() {

        // DB 커넥션을 스프링이 제공하는 DriverManagerDataSource 사용
        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);

        // 의존성 주입
        memberRepository = new MemberRepositoryV3(dataSource);

        // 트랜잭션 매니저 사용
        PlatformTransactionManager transactionManager = new DataSourceTransactionManager(dataSource); // 트랜잭션 매니저에게 dataSource 를 넣어줘야 한다. - 이 dataSource를 통해 커넥션을 생성하겠져..
        memberService = new MemberServiceV3_2(transactionManager, memberRepository); // MemberServiceV3_2 생성 시 의존성 주입
    }

    // 각 테스트 수행 후 호출되는 메소드!! - 테스트 후 정리
    @AfterEach
    void after() throws SQLException {

        // 테스트시 db에 member가 생성되어 테스트를 다시 할때마다 db를 다시 초기화 해줘야 하는 번거로움이 있다.
        // 이를 AfterEach 메소드를 호출하여 각 테스트 메소드 실행 후 호출되도록 하여 자동으로 db 데이터를 지워준다.

        memberRepository.delete(MEMBER_A);
        memberRepository.delete(MEMBER_B);
        memberRepository.delete(MEMBER_EX);

    }

    @Test
    @DisplayName("정상 이체")
    void accountTransfer() throws SQLException {

        // given
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberB = new Member(MEMBER_B, 10000);

        memberRepository.save(memberA);
        memberRepository.save(memberB);

        // when
        memberService.accountTransfer(memberA.getMemberId(), memberB.getMemberId(), 2000);

        // then
        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberB = memberRepository.findById(memberB.getMemberId());

        assertThat(findMemberA.getMoney()).isEqualTo(8000); // memberA : fromId -> 8000원
        assertThat(findMemberB.getMoney()).isEqualTo(12000); // memberB : toId -> 12000원

    }

    @Test
    @DisplayName("이체 중 예외 발생")
    void accountTransferEx() throws SQLException {

        // given
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberEX = new Member(MEMBER_EX, 10000); // memberId 가 EX 면 예외 발생!

        memberRepository.save(memberA);
        memberRepository.save(memberEX);

        // when -> memberService.accountTransfer(memberA.getMemberId(), memberEX.getMemberId(), 2000) 의 수행 결과가 IllegalStateException 이 터져야 정상
        assertThatThrownBy(() -> memberService.accountTransfer(memberA.getMemberId(), memberEX.getMemberId(), 2000))
                .isInstanceOf(IllegalStateException.class);

        // then
        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberB = memberRepository.findById(memberEX.getMemberId());

        assertThat(findMemberA.getMoney()).isEqualTo(10000); // memberA : fromId -> 8000원
        assertThat(findMemberB.getMoney()).isEqualTo(10000); // memberB : toId -> 10000원

        // 이 코드에서는 트랜잭션이 적용되었기 때문에 memberA의 돈이 8000으로 테스트되면
        // 예외가 발생한다. - memberA의 돈도 10000으로 수정해야 한다. 롤백되었기 때문
    }

}