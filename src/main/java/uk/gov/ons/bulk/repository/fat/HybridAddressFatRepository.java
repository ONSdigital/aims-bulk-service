//package uk.gov.ons.bulk.repository.fat;
//
//import org.springframework.data.repository.reactive.ReactiveCrudRepository;
//import org.springframework.stereotype.Repository;
//
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//import uk.gov.ons.bulk.entities.HybridAddressFat;
//
//@Repository
//public interface HybridAddressFatRepository extends ReactiveCrudRepository<HybridAddressFat, String> {
//	
//	Flux<HybridAddressFat> findByLpiNagAllContaining(String search);
//	
//	Mono<HybridAddressFat> findById(String id);
//}
