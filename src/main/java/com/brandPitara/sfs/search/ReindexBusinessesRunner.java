// package com.brandPitara.sfs.search;

// import com.brandPitara.sfs.entity.BusinessEntity;
// import com.brandPitara.sfs.repository.BusinessRepository;
// import lombok.RequiredArgsConstructor;
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.context.annotation.Profile;
// import org.springframework.stereotype.Component;
// import org.springframework.transaction.annotation.Transactional;

// import java.util.List;

// @Component
// @RequiredArgsConstructor
// @Profile("reindex") // will run only when we active this profile
// public class ReindexBusinessesRunner implements CommandLineRunner {

//     private final BusinessRepository businessRepository;
//     private final BusinessSearchService businessSearchService;

//     @Transactional
//     @Override
//     public void run(String... args) throws Exception {
//         System.out.println("🔄 Starting full business reindexing...");

//         List<BusinessEntity> all = businessRepository.findAll();

//         int count = 0;
//         for (BusinessEntity b : all) {
//             businessSearchService.indexBusiness(b);
//             count++;
//         }

//         System.out.println("✅ Reindex complete — " + count + " businesses indexed into Elasticsearch.");
//     }
// }
