import java.util.*;
import java.util.function.*;

/**
 * BEHAVIOURAL DESIGN PATTERNS — explained with MMIS (Medicaid) examples.
 * Pure Java 8.  Run: javac MmisBehaviouralPatterns.java && java MmisBehaviouralPatterns
 *
 * For every pattern you get:
 *   PROBLEM          — the real MMIS situation
 *   WITHOUT PATTERN  — the code people usually write first, and why it hurts
 *   WITH PATTERN     — the runnable code below
 *   WHY THIS, NOT X  — why not the pattern that looks similar
 *   INTERVIEW LINE   — one sentence to say out loud
 *
 * Behavioural patterns answer ONE question: "How do objects talk to each other
 * and who is responsible for what?"
 */
public class MmisBehaviouralPatterns {

    // =========================================================================
    // 1. ★ STRATEGY — "same job, different algorithm, chosen at runtime"
    // =========================================================================
    /*
     * PROBLEM
     *   Every Medicaid claim must be PRICED, but each claim type is priced differently:
     *     - Professional (doctor visit)  -> fee schedule amount
     *     - Institutional (hospital stay)-> per-diem rate x number of days
     *     - Pharmacy                     -> drug cost + dispensing fee
     *   Next year the state adds Dental pricing.
     *
     * WITHOUT PATTERN
     *   double price(Claim c) {
     *       if (c.type.equals("PROF"))      { ... 20 lines ... }
     *       else if (c.type.equals("INST")) { ... 30 lines ... }
     *       else if (c.type.equals("RX"))   { ... 25 lines ... }
     *   }
     *   -> One giant method. Adding Dental means EDITING tested code (risk of breaking PROF).
     *   -> Cannot unit-test each pricing rule in isolation.
     *   -> Violates Open/Closed principle.
     *
     * WITH PATTERN
     *   One interface PricingStrategy, one class per claim type. The service just picks one.
     *   Adding Dental = add ONE new class. Nothing else changes.
     *
     * WHY THIS, NOT ...
     *   - Not TEMPLATE METHOD: the pricing formulas share NO common steps; they are completely
     *     different algorithms. Template is for "same steps, one step differs".
     *   - Not STATE: a claim does not change from PROF to INST over time. The type is fixed and
     *     the CALLER chooses the algorithm. In State, the object switches itself.
     *
     * INTERVIEW LINE
     *   "In claims pricing I'd use Strategy — each claim type gets its own pricing class, and in
     *    Spring I inject Map<String, PricingStrategy> so a new claim type is one new @Component."
     */
    static class Claim {
        final String id, type, memberId, providerId, procedureCode;
        final double billedAmount; final int days;
        Claim(String id, String type, String memberId, String providerId,
              String procedureCode, double billedAmount, int days) {
            this.id = id; this.type = type; this.memberId = memberId; this.providerId = providerId;
            this.procedureCode = procedureCode; this.billedAmount = billedAmount; this.days = days;
        }
    }

    interface PricingStrategy { double price(Claim claim); }

    static class ProfessionalPricing implements PricingStrategy {
        private final Map<String, Double> feeSchedule = new HashMap<>();
        ProfessionalPricing() { feeSchedule.put("99213", 75.0); feeSchedule.put("99214", 110.0); }
        public double price(Claim c) {
            // Medicaid pays the LOWER of billed amount and fee schedule
            return Math.min(c.billedAmount, feeSchedule.getOrDefault(c.procedureCode, 0.0));
        }
    }
    static class InstitutionalPricing implements PricingStrategy {
        private static final double PER_DIEM = 1200.0;
        public double price(Claim c) { return Math.min(c.billedAmount, PER_DIEM * c.days); }
    }
    static class PharmacyPricing implements PricingStrategy {
        private static final double DISPENSING_FEE = 10.50;
        public double price(Claim c) { return c.billedAmount * 0.85 + DISPENSING_FEE; }
    }

    static class ClaimPricingService {
        // In Spring: constructor-injected Map<String, PricingStrategy> keyed by bean name
        private final Map<String, PricingStrategy> strategies = new HashMap<>();
        ClaimPricingService() {
            strategies.put("PROF", new ProfessionalPricing());
            strategies.put("INST", new InstitutionalPricing());
            strategies.put("RX",   new PharmacyPricing());
        }
        double price(Claim c) {
            PricingStrategy s = strategies.get(c.type);
            if (s == null) throw new IllegalArgumentException("No pricing for claim type " + c.type);
            return s.price(c);
        }
    }

    // =========================================================================
    // 2. ★ OBSERVER — "something happened; tell everyone who cares"
    // =========================================================================
    /*
     * PROBLEM
     *   When a provider's enrollment is APPROVED, four things must happen:
     *     send approval letter, index the document in DocFinity, notify the MCO, write an audit log.
     *   Next quarter the business adds "send SMS to provider".
     *
     * WITHOUT PATTERN
     *   void approve(Provider p) {
     *       repo.save(p);
     *       letterService.send(p);        // EnrollmentService now KNOWS about 4 other services
     *       docFinityService.index(p);
     *       mcoService.notify(p);
     *       auditService.log(p);
     *   }
     *   -> Tight coupling. Adding SMS means editing EnrollmentService again.
     *   -> If DocFinity is down, the whole approval fails.
     *
     * WITH PATTERN
     *   EnrollmentService only PUBLISHES "ProviderApproved". Listeners subscribe on their own.
     *   EnrollmentService does not know who is listening.
     *
     * WHY THIS, NOT ...
     *   - Not MEDIATOR: here it is ONE-TO-MANY and ONE-WAY (publisher just announces).
     *     Mediator is MANY-TO-MANY coordination with decision logic in the middle.
     *   - Not CHAIN OF RESPONSIBILITY: in a chain, only one handler may handle it / it can stop.
     *     In Observer, EVERY subscriber receives the event.
     *
     * REAL WORLD
     *   In-process  : Spring ApplicationEventPublisher + @EventListener (@Async for non-blocking)
     *   Distributed : Kafka topic "provider-approved" with many consumer groups (your CMDS POC)
     *
     * INTERVIEW LINE
     *   "Observer decouples the event from its side effects — in our CMDS POC the email
     *    notification was a Kafka consumer, which is Observer at system scale."
     */
    interface ProviderEventListener { void onProviderApproved(String providerId); }

    static class ProviderEnrollmentService {
        private final List<ProviderEventListener> listeners = new ArrayList<>();
        void subscribe(ProviderEventListener l) { listeners.add(l); }

        void approve(String providerId) {
            System.out.println("  Provider " + providerId + " APPROVED and saved");
            for (ProviderEventListener l : listeners) {
                try { l.onProviderApproved(providerId); }
                catch (Exception e) { System.out.println("  Listener failed, approval still OK: " + e.getMessage()); }
            }
        }
    }

    // =========================================================================
    // 3. ★ TEMPLATE METHOD — "fixed steps, only one or two steps differ"
    // =========================================================================
    /*
     * PROBLEM
     *   Claims arrive from three channels: 837 EDI file, provider web portal, scanned paper (OCR).
     *   The processing STEPS are always the same and must happen in this order:
     *     1. read claim   (DIFFERENT per channel)
     *     2. validate     (same)
     *     3. assign ICN   (same — Internal Control Number)
     *     4. save         (same)
     *     5. send acknowledgement (DIFFERENT: 999/277CA file vs portal message vs letter)
     *
     * WITHOUT PATTERN
     *   Three separate classes each copy-paste validate/assignIcn/save.
     *   -> A bug fix in validation must be done in three places. Someone forgets one -> P2 incident.
     *   -> Someone can accidentally save BEFORE validating.
     *
     * WITH PATTERN
     *   Abstract parent owns the ORDER (final method). Subclasses fill only the steps that differ.
     *
     * WHY THIS, NOT ...
     *   - Not STRATEGY: the 3 steps in the middle are IDENTICAL. With Strategy you would
     *     duplicate them in every strategy. Template = reuse via INHERITANCE of shared steps.
     *     Rule of thumb: "skeleton is shared" -> Template;  "whole algorithm swaps" -> Strategy.
     *
     * REAL WORLD
     *   JdbcTemplate: Spring does open connection -> prepare -> execute -> handle exception
     *   -> close; YOU only supply the SQL and RowMapper (the varying step).
     *
     * INTERVIEW LINE
     *   "Template Method protects the order of steps — the parent's method is final, so no
     *    intake channel can skip validation."
     */
    static abstract class ClaimIntakeProcessor {
        private static int icnSequence = 1000;

        public final void process(String rawInput) {          // final = order cannot change
            String claimData = readClaim(rawInput);
            validate(claimData);
            String icn = assignIcn();
            save(claimData, icn);
            sendAcknowledgement(icn);
        }
        protected abstract String readClaim(String rawInput);  // varies
        protected abstract void sendAcknowledgement(String icn); // varies

        private void validate(String data) { System.out.println("  Validate: " + data); }
        private String assignIcn()         { return "ICN" + (++icnSequence); }
        private void save(String d, String icn) { System.out.println("  Saved with " + icn); }
    }
    static class EdiClaimIntake extends ClaimIntakeProcessor {
        protected String readClaim(String raw) { System.out.println("  Parse 837 EDI segments"); return "EDI:" + raw; }
        protected void sendAcknowledgement(String icn) { System.out.println("  Send 277CA file for " + icn); }
    }
    static class PortalClaimIntake extends ClaimIntakeProcessor {
        protected String readClaim(String raw) { System.out.println("  Map portal JSON form"); return "PORTAL:" + raw; }
        protected void sendAcknowledgement(String icn) { System.out.println("  Show " + icn + " on provider portal"); }
    }

    // =========================================================================
    // 4. ★ CHAIN OF RESPONSIBILITY — "pass it along a line of checkers; any can stop it"
    // =========================================================================
    /*
     * PROBLEM
     *   Before payment, every claim runs through EDITS (MMIS term for business validations):
     *     member eligible on date of service -> provider active -> not a duplicate
     *     -> prior authorization present if required -> timely filing (within 365 days)
     *   The first failing edit DENIES the claim with its own reason code.
     *   Policy changes every year: edits are added, removed, reordered.
     *
     * WITHOUT PATTERN
     *   One 300-line validateClaim() method with nested ifs.
     *   -> Reordering edits = risky edit of a huge method.
     *   -> Cannot turn one edit off for one state program.
     *
     * WITH PATTERN
     *   Each edit is a small class. They are linked into a chain. Each one either STOPS
     *   (deny) or PASSES to the next. The chain is assembled from config.
     *
     * WHY THIS, NOT ...
     *   - Not OBSERVER: Observer notifies EVERYONE. A chain can STOP early, and order matters.
     *   - Not DECORATOR: a decorator always calls the wrapped object and ADDS behaviour;
     *     a chain handler may decide NOT to pass the request on.
     *
     * REAL WORLD
     *   Servlet Filters, Spring Security filter chain (JWT filter -> auth -> authorization).
     *
     * INTERVIEW LINE
     *   "Claim edits are a Chain of Responsibility — each edit is independent, testable, and
     *    the order is configuration, so a policy change doesn't touch other edits."
     */
    static abstract class ClaimEdit {
        private ClaimEdit next;
        ClaimEdit then(ClaimEdit next) { this.next = next; return next; }

        String check(Claim c) {                 // returns null if passed, else denial reason
            String failure = apply(c);
            if (failure != null) return failure;
            return next == null ? null : next.check(c);
        }
        protected abstract String apply(Claim c);
    }
    static class EligibilityEdit extends ClaimEdit {
        private final Set<String> eligibleMembers = new HashSet<>(Arrays.asList("M100", "M200"));
        protected String apply(Claim c) {
            System.out.println("    Edit: member eligibility");
            return eligibleMembers.contains(c.memberId) ? null : "DENY 177 - member not eligible";
        }
    }
    static class ProviderActiveEdit extends ClaimEdit {
        protected String apply(Claim c) {
            System.out.println("    Edit: provider active");
            return c.providerId.startsWith("P") ? null : "DENY 205 - provider not active";
        }
    }
    static class DuplicateClaimEdit extends ClaimEdit {
        private final Set<String> paidClaims = new HashSet<>(Collections.singletonList("CLM-OLD"));
        protected String apply(Claim c) {
            System.out.println("    Edit: duplicate check");
            return paidClaims.contains(c.id) ? "DENY 018 - duplicate claim" : null;
        }
    }

    // =========================================================================
    // 5. ★ ITERATOR — "walk through items without knowing how they are stored"
    // =========================================================================
    /*
     * PROBLEM
     *   A nightly job must process 2 million claims. Loading all of them with findAll()
     *   causes OutOfMemoryError. (This is the pagination problem on your resume.)
     *
     * WITHOUT PATTERN
     *   List<Claim> all = repo.findAll();    // 2M objects in heap -> OOM
     *   OR every job writes its own page-loop: offset, limit, "is there a next page?" logic
     *   copy-pasted everywhere, with off-by-one bugs.
     *
     * WITH PATTERN
     *   A ClaimIterator hides the paging. The caller just does for(Claim c : claims).
     *   Internally it fetches the next page only when the current page is used up.
     *
     * WHY THIS, NOT ...
     *   - Not just returning a List: a List must hold everything in memory at once.
     *     The iterator holds ONE page at a time.
     *
     * REAL WORLD
     *   java.util.Iterator, Spring Data Page/Slice, JPA Stream<T> with fetch size,
     *   Spring Batch ItemReader.
     *
     * INTERVIEW LINE
     *   "I hid DB pagination behind an Iterator, so batch jobs stream claims page by page
     *    instead of loading millions of rows and hitting OOM."
     */
    static class PagedClaimIterable implements Iterable<String> {
        private final int totalRows, pageSize;
        PagedClaimIterable(int totalRows, int pageSize) { this.totalRows = totalRows; this.pageSize = pageSize; }

        private List<String> fetchPage(int pageNo) {          // simulates SELECT ... OFFSET/LIMIT
            System.out.println("    [DB] fetching page " + pageNo);
            List<String> page = new ArrayList<>();
            for (int i = pageNo * pageSize; i < Math.min(totalRows, (pageNo + 1) * pageSize); i++) page.add("CLM-" + i);
            return page;
        }

        public Iterator<String> iterator() {
            return new Iterator<String>() {
                private int pageNo = 0, idx = 0;
                private List<String> current = fetchPage(0);
                public boolean hasNext() {
                    if (idx < current.size()) return true;
                    if (current.size() < pageSize) return false;  // last page was partial
                    current = fetchPage(++pageNo); idx = 0;
                    return !current.isEmpty();
                }
                public String next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    return current.get(idx++);
                }
            };
        }
    }

    // =========================================================================
    // 6. COMMAND — "turn an action into an object, so you can queue / log / undo it"
    // =========================================================================
    /*
     * PROBLEM
     *   Claims analysts perform ADJUST and VOID on paid claims. Requirements:
     *     - every action must be AUDITED (who, what, when)
     *     - actions are QUEUED and run in the nightly financial cycle
     *     - a wrong void must be REVERSIBLE
     *
     * WITHOUT PATTERN
     *   Controller calls claimService.voidClaim(id) directly.
     *   -> No record of the action as data, nothing to queue, nothing to reverse.
     *
     * WITH PATTERN
     *   Each action is an object with execute() and undo(). Objects go into a queue and a history.
     *
     * WHY THIS, NOT ...
     *   - Not STRATEGY: Strategy = different WAYS to do the same job (price a claim).
     *     Command = a REQUEST captured as an object, so it can be stored, queued, replayed, undone.
     *
     * REAL WORLD
     *   Runnable / Callable submitted to ExecutorService; Kafka messages are commands too.
     *
     * INTERVIEW LINE
     *   "Command turns 'void this claim' into an object, which gives us audit, queueing for the
     *    financial cycle, and reversal for free."
     */
    interface ClaimCommand { void execute(); void undo(); String describe(); }

    static class ClaimLedger { final Map<String, String> status = new HashMap<>(); }

    static class VoidClaimCommand implements ClaimCommand {
        private final ClaimLedger ledger; private final String claimId; private String previous;
        VoidClaimCommand(ClaimLedger l, String id) { ledger = l; claimId = id; }
        public void execute() { previous = ledger.status.put(claimId, "VOIDED"); System.out.println("  " + claimId + " -> VOIDED"); }
        public void undo()    { ledger.status.put(claimId, previous); System.out.println("  " + claimId + " restored to " + previous); }
        public String describe() { return "VOID " + claimId; }
    }

    static class FinancialCycle {
        private final Queue<ClaimCommand> pending = new LinkedList<>();
        private final Deque<ClaimCommand> history = new ArrayDeque<>();
        void submit(ClaimCommand c) { pending.add(c); System.out.println("  Queued: " + c.describe() + " (audit logged)"); }
        void runNightly() { while (!pending.isEmpty()) { ClaimCommand c = pending.poll(); c.execute(); history.push(c); } }
        void reverseLast() { if (!history.isEmpty()) history.pop().undo(); }
    }

    // =========================================================================
    // 7. STATE — "the object behaves differently depending on its current status"
    // =========================================================================
    /*
     * PROBLEM
     *   A Prior Authorization (PA) moves: SUBMITTED -> IN_REVIEW -> APPROVED or DENIED.
     *   Rules:
     *     - Only IN_REVIEW can be approved or denied
     *     - APPROVED / DENIED are final
     *     - A SUBMITTED PA cannot be approved directly
     *
     * WITHOUT PATTERN
     *   void approve() {
     *       if (status == SUBMITTED) throw ...;
     *       else if (status == IN_REVIEW) status = APPROVED;
     *       else if (status == APPROVED) throw ...;
     *       else if (status == DENIED) throw ...;
     *   }
     *   ...and the same switch repeated in deny(), review(), cancel()...
     *   -> Adding a new status ("PENDING_DOCS") means editing every method.
     *
     * WITH PATTERN
     *   Each status is a class that knows which actions are allowed from it.
     *
     * WHY THIS, NOT ...
     *   - Not STRATEGY: the code looks similar, but in Strategy the CALLER picks the behaviour
     *     once. In State, the OBJECT moves itself from state to state after each action.
     *   - For 2–3 simple states an enum is fine; use State when each state has real rules.
     *
     * REAL WORLD
     *   Spring State Machine; claim status PAID/PENDED/DENIED; order lifecycle.
     *
     * INTERVIEW LINE
     *   "State removes the status switch from every method — each PA status class owns its
     *    own allowed transitions, so invalid moves are impossible by design."
     */
    interface PaState {
        default PaState review()  { throw new IllegalStateException("Cannot review from " + name()); }
        default PaState approve() { throw new IllegalStateException("Cannot approve from " + name()); }
        default PaState deny()    { throw new IllegalStateException("Cannot deny from " + name()); }
        String name();
    }
    static class Submitted implements PaState {
        public PaState review() { return new InReview(); }
        public String name() { return "SUBMITTED"; }
    }
    static class InReview implements PaState {
        public PaState approve() { return new Approved(); }
        public PaState deny()    { return new Denied(); }
        public String name() { return "IN_REVIEW"; }
    }
    static class Approved implements PaState { public String name() { return "APPROVED"; } }
    static class Denied   implements PaState { public String name() { return "DENIED"; } }

    static class PriorAuthorization {
        private PaState state = new Submitted();
        void apply(String action, Function<PaState, PaState> transition) {
            try {
                String from = state.name();
                state = transition.apply(state);
                System.out.println("  " + action + ": " + from + " -> " + state.name());
            } catch (IllegalStateException e) {
                System.out.println("  " + action + " BLOCKED: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // 8. MEDIATOR — "one coordinator in the middle; components never call each other"
    // =========================================================================
    /*
     * PROBLEM
     *   Processing one claim needs: Eligibility, Provider, Pricing, Payment services.
     *   Pricing needs eligibility result; Payment needs pricing result; if eligibility fails,
     *   Payment must not run.
     *
     * WITHOUT PATTERN
     *   EligibilityService calls ProviderService, which calls PricingService, which calls
     *   PaymentService...  -> a web of dependencies, circular calls, impossible to test alone.
     *
     * WITH PATTERN
     *   A ClaimProcessingMediator holds the flow and decisions. Each service only does its job
     *   and knows nothing about the others.
     *
     * WHY THIS, NOT ...
     *   - Not OBSERVER: Observer just broadcasts and doesn't care about results. A mediator
     *     READS results and DECIDES what happens next (two-way, with logic).
     *   - Not FACADE: a facade only simplifies calls for an outside client; a mediator
     *     coordinates components that would otherwise talk to each other.
     *
     * REAL WORLD
     *   Saga ORCHESTRATOR in microservices; Spring DispatcherServlet.
     *
     * INTERVIEW LINE
     *   "The claim orchestrator is a Mediator — services stay independent, and the flow and
     *    failure decisions live in one place. In microservices that's an orchestrated Saga."
     */
    static class EligibilityCheck { boolean isEligible(String memberId) { return !memberId.equals("M999"); } }
    static class PaymentService   { void pay(String claimId, double amt) { System.out.println("  Payment issued: " + claimId + " = " + amt); } }

    static class ClaimProcessingMediator {
        private final EligibilityCheck eligibility = new EligibilityCheck();
        private final ClaimPricingService pricing = new ClaimPricingService();
        private final PaymentService payment = new PaymentService();

        void process(Claim c) {
            System.out.println("  Processing " + c.id);
            if (!eligibility.isEligible(c.memberId)) {
                System.out.println("  Member not eligible -> claim DENIED, payment skipped");
                return;
            }
            double allowed = pricing.price(c);
            payment.pay(c.id, allowed);
        }
    }

    // =========================================================================
    // 9. MEMENTO — "take a snapshot so you can roll back"
    // =========================================================================
    /*
     * PROBLEM
     *   An analyst ADJUSTS a paid claim (changes units and amount). If the re-adjudication
     *   fails half-way, the claim must go back exactly to how it was. Also the "before"
     *   snapshot is needed for the audit report.
     *
     * WITHOUT PATTERN
     *   Copy each field into local variables before the change, restore them one by one.
     *   -> Forget one field and the claim is corrupted. Other classes start reading internals.
     *
     * WITH PATTERN
     *   The claim creates its own immutable snapshot (memento) and can restore from it.
     *   Nobody else can see or edit what's inside the snapshot.
     *
     * WHY THIS, NOT ...
     *   - Not COMMAND undo: Command undo reverses ONE action using logic.
     *     Memento restores the WHOLE state at once. They are often used together.
     *   - A DB transaction rollback covers the database; Memento covers the in-memory object.
     *
     * INTERVIEW LINE
     *   "Before adjusting a claim we snapshot it as a memento — if re-adjudication fails we
     *    restore it, and the snapshot doubles as the audit 'before' record."
     */
    static class AdjustableClaim {
        private int units; private double amount;
        AdjustableClaim(int units, double amount) { this.units = units; this.amount = amount; }
        void adjust(int newUnits, double newAmount) { units = newUnits; amount = newAmount; }
        Snapshot snapshot() { return new Snapshot(units, amount); }
        void restore(Snapshot s) { units = s.units; amount = s.amount; }
        public String toString() { return "units=" + units + ", amount=" + amount; }

        static final class Snapshot {                         // immutable, private fields
            private final int units; private final double amount;
            private Snapshot(int u, double a) { units = u; amount = a; }
        }
    }

    // =========================================================================
    // 10. VISITOR — "add new operations across many classes without touching them"
    // =========================================================================
    /*
     * PROBLEM
     *   We have claim classes: ProfessionalClaim, InstitutionalClaim, PharmacyClaim.
     *   CMS and the state keep asking for NEW reports over them: total paid, federal share
     *   (FMAP), T-MSIS export format... The claim classes are stable; the operations keep growing.
     *
     * WITHOUT PATTERN
     *   for (Object c : claims) {
     *       if (c instanceof ProfessionalClaim) ... else if (c instanceof InstitutionalClaim) ...
     *   }   -> this instanceof chain is copied into every report.
     *   OR add calculateFederalShare(), exportTmsis(), ... into every claim class
     *   -> claim classes bloat with reporting logic.
     *
     * WITH PATTERN
     *   Each new report is ONE new Visitor class. Claim classes never change.
     *
     * WHY THIS, NOT ...
     *   - Not STRATEGY: Strategy swaps one algorithm for ONE type. Visitor applies one operation
     *     across MANY different types, with type-specific logic for each.
     *   - Trade-off to mention: adding a NEW claim type is painful (every visitor must change).
     *     So use Visitor when types are stable and operations change often.
     *
     * INTERVIEW LINE
     *   "Visitor fits when the class hierarchy is stable but operations keep growing — like
     *    new CMS reports over fixed claim types."
     */
    interface ClaimVisitor { void visit(ProfClaim c); void visit(InstClaim c); }
    interface VisitableClaim { void accept(ClaimVisitor v); }

    static class ProfClaim implements VisitableClaim {
        final double paid; ProfClaim(double p) { paid = p; }
        public void accept(ClaimVisitor v) { v.visit(this); }
    }
    static class InstClaim implements VisitableClaim {
        final double paid; final int days; InstClaim(double p, int d) { paid = p; days = d; }
        public void accept(ClaimVisitor v) { v.visit(this); }
    }
    static class FederalShareReport implements ClaimVisitor {
        private static final double FMAP = 0.65;   // federal match %
        double total;
        public void visit(ProfClaim c) { total += c.paid * FMAP; }
        public void visit(InstClaim c) { total += c.paid * FMAP; }
    }
    static class InpatientDaysReport implements ClaimVisitor {
        int days;
        public void visit(ProfClaim c) { /* not applicable */ }
        public void visit(InstClaim c) { days += c.days; }
    }

    // =========================================================================
    // 11. INTERPRETER — "rules written as data, evaluated by code"
    // =========================================================================
    /*
     * PROBLEM
     *   Eligibility rules change with every state policy update, e.g.
     *     "age < 19 AND income <= 200% FPL"   (children's program)
     *     "age >= 65 OR disabled"            (aged/blind/disabled)
     *   Business wants to change these without a code release.
     *
     * WITHOUT PATTERN
     *   Hard-coded if conditions -> every policy change = code change + deploy.
     *
     * WITH PATTERN
     *   Small classes for each grammar piece (comparison, AND, OR) combine into a rule tree
     *   that can be built from config and evaluated against a member.
     *
     * WHY THIS, NOT ...
     *   - Not CHAIN: a chain runs checks in sequence. Interpreter builds a TREE with AND/OR logic.
     *   - In real projects you'd usually use an existing engine (Drools, SpEL) instead of
     *     writing your own — say that in the interview.
     *
     * INTERVIEW LINE
     *   "Eligibility rules are a grammar — Interpreter lets policy live in config. In practice
     *    I'd use SpEL or Drools, which are built on this pattern."
     */
    static class Member {
        final int age; final double incomePctFpl; final boolean disabled;
        Member(int age, double income, boolean disabled) { this.age = age; this.incomePctFpl = income; this.disabled = disabled; }
    }
    interface Rule { boolean evaluate(Member m); }

    static Rule ageBelow(int n)            { return m -> m.age < n; }
    static Rule ageAtLeast(int n)          { return m -> m.age >= n; }
    static Rule incomeAtMost(double pct)   { return m -> m.incomePctFpl <= pct; }
    static Rule isDisabled()               { return m -> m.disabled; }
    static Rule and(Rule a, Rule b)        { return m -> a.evaluate(m) && b.evaluate(m); }
    static Rule or(Rule a, Rule b)         { return m -> a.evaluate(m) || b.evaluate(m); }

    // =========================================================================
    // DEMO
    // =========================================================================
    public static void main(String[] args) {
        Claim prof = new Claim("CLM-1", "PROF", "M100", "P10", "99213", 120.0, 0);
        Claim inst = new Claim("CLM-2", "INST", "M200", "P20", "",      5000.0, 3);
        Claim rx   = new Claim("CLM-3", "RX",   "M100", "P30", "",      40.0,   0);

        title("1. STRATEGY - price claims by type");
        ClaimPricingService pricing = new ClaimPricingService();
        for (Claim c : Arrays.asList(prof, inst, rx))
            System.out.printf("  %s (%s) billed %.2f -> allowed %.2f%n", c.id, c.type, c.billedAmount, pricing.price(c));

        title("2. OBSERVER - provider approved, everyone reacts");
        ProviderEnrollmentService enrollment = new ProviderEnrollmentService();
        enrollment.subscribe(id -> System.out.println("  Letter: approval letter mailed to " + id));
        enrollment.subscribe(id -> System.out.println("  DocFinity: enrollment docs indexed for " + id));
        enrollment.subscribe(id -> { throw new RuntimeException("MCO service down"); });
        enrollment.subscribe(id -> System.out.println("  Audit: approval logged for " + id));
        enrollment.approve("P10");

        title("3. TEMPLATE METHOD - same intake steps, different channels");
        new EdiClaimIntake().process("ISA*00*...");
        new PortalClaimIntake().process("{claim json}");

        title("4. CHAIN OF RESPONSIBILITY - claim edits");
        ClaimEdit edits = new EligibilityEdit();
        edits.then(new ProviderActiveEdit()).then(new DuplicateClaimEdit());
        Claim good = prof;
        Claim notEligible = new Claim("CLM-4", "PROF", "M555", "P10", "99213", 100, 0);
        Claim duplicate   = new Claim("CLM-OLD", "PROF", "M100", "P10", "99213", 100, 0);
        for (Claim c : Arrays.asList(good, notEligible, duplicate)) {
            System.out.println("  " + c.id + ":");
            String result = edits.check(c);
            System.out.println("    RESULT: " + (result == null ? "PASSED all edits" : result));
        }

        title("5. ITERATOR - nightly job reads 7 claims, 3 per page");
        for (String id : new PagedClaimIterable(7, 3)) System.out.println("  processing " + id);

        title("6. COMMAND - queue a void, run nightly, reverse it");
        ClaimLedger ledger = new ClaimLedger();
        ledger.status.put("CLM-1", "PAID");
        FinancialCycle cycle = new FinancialCycle();
        cycle.submit(new VoidClaimCommand(ledger, "CLM-1"));
        cycle.runNightly();
        cycle.reverseLast();

        title("7. STATE - prior authorization lifecycle");
        PriorAuthorization pa = new PriorAuthorization();
        pa.apply("approve", PaState::approve);   // not allowed yet
        pa.apply("review",  PaState::review);
        pa.apply("approve", PaState::approve);
        pa.apply("deny",    PaState::deny);      // already final

        title("8. MEDIATOR - claim orchestrator");
        ClaimProcessingMediator mediator = new ClaimProcessingMediator();
        mediator.process(prof);
        mediator.process(new Claim("CLM-5", "PROF", "M999", "P10", "99214", 150, 0));

        title("9. MEMENTO - adjust a claim, roll back on failure");
        AdjustableClaim claim = new AdjustableClaim(2, 150.0);
        AdjustableClaim.Snapshot before = claim.snapshot();
        claim.adjust(5, 375.0);
        System.out.println("  After adjust:  " + claim);
        System.out.println("  Re-adjudication FAILED -> restore");
        claim.restore(before);
        System.out.println("  After restore: " + claim);

        title("10. VISITOR - new reports, claim classes untouched");
        List<VisitableClaim> paidClaims = Arrays.asList(new ProfClaim(100), new InstClaim(3600, 3), new InstClaim(2400, 2));
        FederalShareReport fed = new FederalShareReport();
        InpatientDaysReport days = new InpatientDaysReport();
        paidClaims.forEach(c -> { c.accept(fed); c.accept(days); });
        System.out.printf("  Federal share total = %.2f%n", fed.total);
        System.out.println("  Inpatient days total = " + days.days);

        title("11. INTERPRETER - eligibility rules as a tree");
        Rule childProgram = and(ageBelow(19), incomeAtMost(200));
        Rule abdProgram   = or(ageAtLeast(65), isDisabled());
        Member child  = new Member(10, 150, false);
        Member senior = new Member(70, 400, false);
        System.out.println("  Child (10, 150% FPL) in children's program? " + childProgram.evaluate(child));
        System.out.println("  Senior (70, 400% FPL) in children's program? " + childProgram.evaluate(senior));
        System.out.println("  Senior in aged/blind/disabled program?       " + abdProgram.evaluate(senior));
    }

    static void title(String t) { System.out.println("\n=== " + t + " ==="); }
}
