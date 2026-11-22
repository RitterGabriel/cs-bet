package com.csbet.backend.skin;

import com.csbet.backend.casecatalog.SourceDataException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AgentCatalogTest {
    @Test void bundledAgentsHaveStableIdsTeamsRaritiesAndMarketNames() {
        var agents=AgentCatalogSource.bundled();
        assertThat(agents).hasSize(63);
        assertThat(agents.stream().map(AgentCatalogSource.Agent::id).distinct()).hasSize(63);
        assertThat(agents.stream().map(AgentCatalogSource.Agent::teamId).collect(java.util.stream.Collectors.toSet()))
                .containsExactlyInAnyOrder("terrorists","counter-terrorists");
        assertThat(agents).allSatisfy(agent->{assertThat(agent.id()).startsWith("agent-");assertThat(agent.rarity().id()).endsWith("_character");assertThat(agent.marketHashName()).isNotBlank();});
    }
    @Test void malformedOrDuplicatedAgentDataIsRejectedBeforeImport() throws Exception {
        var mapper=new JsonMapper();
        try(var input=getClass().getResourceAsStream("/catalog/agents.json")) {
            String item=mapper.readTree(input).get(0).toString();
            for(var invalid:List.of("[]","["+item+","+item+"]","["+item.replace("agent-4613","skin-4613")+"]",
                    "["+item.replace("\"terrorists\"","\"unknown\"")+"]", "["+item.replace("https://community.akamai.steamstatic.com","https://invalid.example")+"]")) {
                assertThatThrownBy(()->AgentCatalogSource.read(new ByteArrayInputStream(invalid.getBytes(StandardCharsets.UTF_8))))
                        .isInstanceOf(SourceDataException.class);
            }
        }
    }
    @Test void importIsIdempotentAndPreservesExistingWeaponItems() {
        var repository=mock(CosmeticItemRepository.class);var rarities=mock(RarityRepository.class);
        Map<String,CosmeticItem> store=new HashMap<>();Map<String,Rarity> grades=new HashMap<>();var sequence=new AtomicLong(1);
        var existing=new CosmeticItem("BYMYKEL","skin-existing");existing.update("Existing weapon","AK-47","rifles","none",null,new Rarity("rare","Rare","#4b69ff"));
        ReflectionTestUtils.setField(existing,"id",1L);store.put(existing.getExternalId(),existing);
        when(repository.findAllBySource("BYMYKEL")).thenAnswer(call->new ArrayList<>(store.values()));
        when(rarities.findAll()).thenAnswer(call->new ArrayList<>(grades.values()));
        when(rarities.saveAll(any())).thenAnswer(call->{List<Rarity> result=new ArrayList<>();for(var grade:call.<Iterable<Rarity>>getArgument(0)){grades.put(grade.getId(),grade);result.add(grade);}return result;});
        when(repository.save(any())).thenAnswer(call->{CosmeticItem item=call.getArgument(0);if(item.getId()==null)ReflectionTestUtils.setField(item,"id",sequence.incrementAndGet());store.put(item.getExternalId(),item);return item;});
        var service=new AgentCatalogImportService(repository,rarities);var source=AgentCatalogSource.bundled();
        assertThat(service.importAgents(source)).isEqualTo(63);assertThat(service.importAgents(source)).isEqualTo(63);
        assertThat(store).hasSize(64);assertThat(store.get("skin-existing").getName()).isEqualTo("Existing weapon");
        assertThat(store.values().stream().filter(i->i.getWeaponType().equals("agent"))).hasSize(63);
        verify(repository,times(2)).flush();
    }
    @Test void dryRunDoesNotStartSpringOrNeedOracleOrJwtCredentials() {
        assertThat(AgentCatalogImportApplication.run(new String[]{"--agents-import","--agents.dry-run=true"})).isZero();
    }
}
