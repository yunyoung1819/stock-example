# stock-example
인프런 재고시스템으로 알아보는 동시성 이슈 해결방법

## 1. 작업 환경 셋팅
docker 설치
- brew install docker
- brew link docker
- docker version

mysql 설치 및 실행
- docker pull mysql
- docker run -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=1234 --name mysql mysql
- docker ps

mysql 데이터베이스 생성
- docker exec -it mysql bash
- mysql -u root -p
- create database stock_example;
- use stock_example;

## 2. 재고 감소 로직
### 100개의 재고 감소 요청이 들어오면?
### 예상한 결과

| Thread-1                                        | Stock                | Thread-2                                      |
|-------------------------------------------------|----------------------|-----------------------------------------------|
| select * from stock where id = 1                | {id: 1, quantity: 5} |                                               |
| update set quantity = 4 from stock where id = 1 | {id: 1, quantity: 4} |                                               |
|                                                 | {id: 1, quantity: 4} | select * from stock where id = 1              |
|                                                 | {id: 1, quantity: 3} | update set quantity = 3 from stock where id = 1 |

### 실제의 결과
| Thread-1                                        | Stock                | Thread-2                                        |
|-------------------------------------------------|----------------------|-------------------------------------------------|
| select * from stock where id = 1                | {id: 1, quantity: 5} |                                                 |
|                                                 | {id: 1, quantity: 5} | select * from stock where id 1                  |
| update set quantity = 4 from stock where id = 1 | {id: 1, quantity: 4} |                                                 |
|                                                 | {id: 1, quantity: 4} | update set quantity = 4 from stock where id = 1 |


## 3. Synchronized 이용하기
- Synchronized를 사용했을 때 발생할 수 있는 문제점
  - 자바의 synchronized는 하나의 프로세스 안에서만 보장이 된다.
  - 서버가 1대일때는 데어터의 접근을 서버가 1대만 해서 괜찮겠지만 서버가 2대 또는 그 이상일 경우에는 데이터의 접근을 여러 서버에서 할 수 있게 된다.
  - synchronized는 각 프로세스 안에서만 보장이 되기 때문에 결국 여러 스레드에서 동시에 데이터에 접근할 수 있게 되면서 race condition이 발생할 수 있음
  - 실제 운영 중인 서비스는 대부분 2대 이상의 서버를 사용하기 때문에 Synchronized는 거의 사용되지 않음

## 4. MySQL을 활용한 다양한 방법
1. Pessimistic Lock (비관적 락)
- 실제로 데이터에 Lock을 걸어서 정합성을 맞추는 방법
- exclusive lock을 걸게되면 다른 트랜잭션에서는 lock이 해제되기 전에 데이터를 가져갈 수 없음
- 데드락이 걸릴 수 있기 때문에 주의해서 사용해야함

2. Optimistic Lock (낙관적 락)
- 실제로 Lock을 이용하지 않고 버전을 이용함으로써 정합성을 맞추는 방법
- 먼저 데이터를 읽은 후에 update를 수행할 때 현재 내가 읽은 버전이 맞는지 확인하여 업데이트 한다.
- 내가 읽은 버전에서 수정사항이 생겼을 경우에는 aplication에서 다시 읽은 후에 작업을 수행해야 함

3. Named Lock
- 이름을 가진 metadata locking 이다.
- 이름을 가진 lock을 획득한 후 해제할 때까지 다른 세션은 이 lock을 획득할 수 없도록 함
- 주의할 점으로는 transaction이 종료될 때 lock이 자동으로 해제되지 않음
- 별도의 명렁어로 해제를 수행해주거나 선점시간이 끝나야 해제됨

### Pessimistic Lock

![](./images/비관적락.png)
- 실제로 데이터에 Lock을 걸어서 정합성을 맞추는 방법
- Spring Data JPA에서는 @Lock(LockModeType.PESSIMISTIC_WRITE)를 이용

### Optimistic Lock
![](./images/optimisticlock.png)
- Optimistic Lock은 실제로 `Lock`을 이옹하지 않고 `버전`을 이용함으로써 정합성을 맞추는 방법

### Named Lock
![](./images/namedlock.png)
- 이름을 가진 meta data lock 이다.
- 이름을 가진 lock을 획득한 후 해제할 때까지 다른 세션은 이 락을 획득할 수 없음
- 트랜잭션이 종료될 때 자동으로 해제되지 않기에 별도의 명령어로 해제하거나 선점 시간이 끝나야 해제
- MySQL에서는 get_lock이라는 명령어로 lock을 획득하고 release라는 명령어로 lock을 해제

### 출처
- [인프런] 재고시스템으로 알아보는 동시성 이슈 해결 방법 강의