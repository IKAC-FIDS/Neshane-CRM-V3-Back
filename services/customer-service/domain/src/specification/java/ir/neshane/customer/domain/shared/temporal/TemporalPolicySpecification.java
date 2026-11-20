package ir.neshane.customer.domain.shared.temporal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import static ir.neshane.customer.domain.shared.temporal.TemporalPolicy.EndBoundary.*;
import static ir.neshane.customer.domain.shared.temporal.TemporalPolicy.Position.*;

public final class TemporalPolicySpecification {
    private static int checks;
    public static void run() {
        var start=LocalDate.of(2024,2,28); var end=start.plusDays(2);
        var instant=Instant.parse("2024-02-28T23:59:59Z"); var expiry=instant.plusSeconds(2);
        var context=new TemporalPolicy.Context(start,instant);
        expect(context.businessDate().equals(start)); expect(context.asOf().equals(instant));
        expect(new TemporalPolicy.Context(start.plusDays(1),instant).asOf().equals(instant));
        for (var boundary:TemporalPolicy.EndBoundary.values()) {
            expect(TemporalPolicy.position(start,Optional.of(end),start.minusDays(1),boundary)==BEFORE_START);
            expect(TemporalPolicy.position(start,Optional.of(end),start,boundary)==WITHIN);
            expect(TemporalPolicy.position(start,Optional.of(end),start.plusDays(1),boundary)==WITHIN);
            expect(TemporalPolicy.position(start,Optional.of(end),end.plusDays(1),boundary)==AFTER_END);
            expect(TemporalPolicy.position(start,Optional.empty(),LocalDate.MAX,boundary)==WITHIN);
            expect(TemporalPolicy.position(instant,Optional.of(expiry),instant.minusNanos(1),boundary)==BEFORE_START);
            expect(TemporalPolicy.position(instant,Optional.of(expiry),instant,boundary)==WITHIN);
            expect(TemporalPolicy.position(instant,Optional.of(expiry),expiry.minusNanos(1),boundary)==WITHIN);
            expect(TemporalPolicy.position(instant,Optional.of(expiry),expiry.plusNanos(1),boundary)==AFTER_END);
            expect(TemporalPolicy.position(instant,Optional.empty(),Instant.MAX,boundary)==WITHIN);
            reject("TEMPORAL_INTERVAL_INVALID",()->TemporalPolicy.position(start,Optional.of(start.minusDays(1)),start,boundary));
        }
        expect(TemporalPolicy.position(start,Optional.of(end),end,EXCLUSIVE)==AFTER_END);
        expect(TemporalPolicy.position(start,Optional.of(end),end,INCLUSIVE)==WITHIN);
        expect(TemporalPolicy.position(start,Optional.of(end),end,UNRESOLVED)==BOUNDARY_UNRESOLVED);
        expect(TemporalPolicy.position(instant,Optional.of(expiry),expiry,EXCLUSIVE)==AFTER_END);
        expect(TemporalPolicy.position(instant,Optional.of(expiry),expiry,INCLUSIVE)==WITHIN);
        expect(TemporalPolicy.position(instant,Optional.of(expiry),expiry,UNRESOLVED)==BOUNDARY_UNRESOLVED);
        expect(TemporalPolicy.position(start,Optional.of(start),start,INCLUSIVE)==WITHIN);
        reject("TEMPORAL_INTERVAL_INVALID",()->TemporalPolicy.position(start,Optional.of(start),start,EXCLUSIVE));
        reject("TEMPORAL_INTERVAL_INVALID",()->TemporalPolicy.position(start,Optional.of(start),start,UNRESOLVED));
        reject("BUSINESS_DATE_REQUIRED",()->new TemporalPolicy.Context(null,instant));
        reject("EVALUATION_INSTANT_REQUIRED",()->new TemporalPolicy.Context(start,null));
        reject("TEMPORAL_START_REQUIRED",()->TemporalPolicy.position(null,Optional.of(end),start,EXCLUSIVE));
        reject("TEMPORAL_END_OPTIONAL_REQUIRED",()->TemporalPolicy.position(start,null,start,EXCLUSIVE));
        reject("TEMPORAL_AS_OF_REQUIRED",()->TemporalPolicy.position(start,Optional.of(end),null,EXCLUSIVE));
        reject("TEMPORAL_BOUNDARY_REQUIRED",()->TemporalPolicy.position(start,Optional.of(end),start,null));
        expect(TemporalPolicy.position(LocalDate.MAX,Optional.empty(),LocalDate.MAX,EXCLUSIVE)==WITHIN);
        expect(TemporalPolicy.position(Instant.MAX,Optional.empty(),Instant.MAX,EXCLUSIVE)==WITHIN);
        System.out.println("PASS: "+checks+" isolated temporal policy foundation assertions");
    }
    private static void reject(String code,Runnable action) { try { action.run(); } catch (IllegalArgumentException | NullPointerException ex) { expect(code.equals(ex.getMessage())); return; } throw new AssertionError("Expected "+code); }
    private static void expect(boolean v) { if (!v) throw new AssertionError("Temporal policy assertion "+(checks+1)); checks++; }
}
