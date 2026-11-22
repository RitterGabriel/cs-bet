package com.csbet.backend.pricing;
import com.csbet.backend.BackendApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

public final class PriceUpdateApplication {
    private PriceUpdateApplication() { }
    public static int run(String[] args) {
        // Sem fonte autorizada não há tráfego nem retorno aos preços artificiais antigos.
        boolean enabled = Boolean.parseBoolean(System.getenv("MARKET_QUOTES_ENABLED"))
                || java.util.Arrays.asList(args).contains("--csbet.market.enabled=true");
        if (!enabled) {
            System.out.println("Cotações desativadas: nenhuma fonte autorizada configurada. Nenhum preço foi gerado.");
            return 0;
        }
        // Não habilita Flyway: a migração continua sendo uma operação explícita.
        try (var context = new SpringApplicationBuilder(BackendApplication.class)
                .web(WebApplicationType.NONE).profiles("price-update").run(args)) {
            String status = context.getBean(com.csbet.backend.market.MarketRefreshService.class).refresh();
            System.out.println("Atualização de referências de mercado: " + status);
            return status.equals("FAILED") || status.equals("INVALID_CONFIG") || status.equals("REJECTED") ? 1 : 0;
        } catch (Exception exception) {
            System.err.println(failureMessage(exception));
            return 1;
        }
    }
    // Mostra a categoria/tipo da falha, sem despejar mensagens JDBC que podem conter dados sensíveis.
    static String failureMessage(Throwable exception) {
        var visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable, Boolean>());
        Throwable root = exception;
        Throwable orm = null;
        java.sql.SQLException sql = null;
        boolean schema = false;
        boolean connection = false;
        boolean integrity = false;
        for (Throwable cause = exception; cause != null && visited.add(cause); cause = cause.getCause()) {
            root = cause;
            if (cause instanceof org.hibernate.HibernateException || cause instanceof org.hibernate.AssertionFailure) orm = cause;
            if (cause instanceof java.sql.SQLException database) sql = database;
            schema |= cause instanceof org.hibernate.tool.schema.spi.SchemaManagementException;
            connection |= cause instanceof org.hibernate.exception.JDBCConnectionException
                    || cause instanceof org.springframework.jdbc.CannotGetJdbcConnectionException;
            integrity |= cause instanceof org.hibernate.exception.ConstraintViolationException
                    || cause instanceof org.springframework.dao.DataIntegrityViolationException;
        }
        String prefix = "Atualização de preços não concluída. ";
        if (schema) return prefix + "Falha de validação entre o mapeamento JPA e o schema. Confira as divergências indicadas pelo Hibernate; não reaplique migrations já executadas.";
        if (connection || (sql != null && sql.getSQLState() != null && sql.getSQLState().startsWith("08"))) {
            return prefix + "Falha de comunicação com o Oracle. Confira a disponibilidade do serviço e a configuração de conexão.";
        }
        if (integrity) return prefix + "Violação de integridade ao gravar os preços. Confira as constraints e o diagnóstico da transação.";
        if (orm != null) return prefix + "Falha de persistência Hibernate (" + orm.getClass().getSimpleName()
                + "). Confira o mapeamento e a operação de persistência no diagnóstico do ORM.";
        if (sql != null) return prefix + "O Oracle rejeitou uma operação SQL (código " + sql.getErrorCode() + "). Confira o diagnóstico do banco.";
        return prefix + "Falha em " + root.getClass().getSimpleName() + ". Consulte o diagnóstico da execução para identificar a causa.";
    }

}
