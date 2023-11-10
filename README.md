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


## 3. v1. Synchronized 이용하기
- Synchronized를 사용했을 때 발생할 수 있는 문제점
  - 자바의 synchronized는 하나의 프로세스 안에서만 보장이 된다.
  - 서버가 1대일때는 데어터의 접근을 서버가 1대만 해서 괜찮겠지만 서버가 2대 또는 그 이상일 경우에는 데이터의 접근을 여러 서버에서 할 수 있게 된다.
  - synchronized는 각 프로세스 안에서만 보장이 되기 때문에 결국 여러 스레드에서 동시에 데이터에 접근할 수 있게 되면서 race condition이 발생할 수 있음
  - 실제 운영 중인 서비스는 대부분 2대 이상의 서버를 사용하기 때문에 Synchronized는 거의 사용되지 않음

