package com.example.stock.facade;

import com.example.stock.service.OptimisticLockStockService;
import org.springframework.stereotype.Component;

@Component
public class OptimisticLockStockFacade {

	private final OptimisticLockStockService optimisticLockStockService;

	public OptimisticLockStockFacade(OptimisticLockStockService optimisticLockStockService) {
		this.optimisticLockStockService = optimisticLockStockService;
	}

	public void decrease(Long id, Long quantity) throws InterruptedException {
		// 업데이트를 실패했을 때 재시도하는 로직
		while (true) {
			try {
				optimisticLockStockService.decrease(id, quantity);

				break;
			} catch (Exception e) {
				Thread.sleep(50);
			}
		}
	}

}
