package iminhak;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.events.triggers.marks.adv.SpecificAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@CalloutRepo(name = "P8S Automarkers", duty = KnownDuty.P8S)
public class P8SAutoMarkers extends AutoChildEventHandler {

    private static final Logger log = LoggerFactory.getLogger(P8SAutoMarkers.class);

    private final BooleanSetting useAutoMarks;

    private final BooleanSetting useLimitlessDesolation;
    private final BooleanSetting LDSupportAttack;

    private final BooleanSetting useHC1;

    private final BooleanSetting useHC2;

    private final BooleanSetting useLD;

    public P8SAutoMarkers(XivState state, StatusEffectRepository buffs, PersistenceProvider pers) {
        this.useAutoMarks = new BooleanSetting(pers, "triggers.p8s.use-auto-markers", false);

        this.useLimitlessDesolation = new BooleanSetting(pers, "triggers.p8s.use-dominion", false);
        this.LDSupportAttack = new BooleanSetting(pers, "triggers.p8s.use-LD-sup-attack", false);

        this.useHC1 = new BooleanSetting(pers, "triggers.p8s.use-hc1", false);

        this.useHC2 = new BooleanSetting(pers, "triggers.p8s.use-hc2", false);

        this.useLD = new BooleanSetting(pers, "triggers.p8s.use-ld", false);
    }

    @AutoFeed
    private final SequentialTrigger<BaseEvent> limitlessDesolation = SqtTemplates.sq(60_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(0x75ED),
            (e1, s) -> {
                if(getUseAutoMarks().get() && getUseLimitlessDesolation().get()) {
                    log.info("Limitless Desolation markers are enabled");
                    for (int i = 1; i <= 4; i++) {
                        boolean inverse = getLDSupportAttack().get();
                        List<AbilityUsedEvent> events = s.waitEventsQuickSuccession(2, AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x75F0) && aue.isFirstTarget(), Duration.ofMillis(400));
                        //Expect 1 sup and 1 dps
                        Optional<AbilityUsedEvent> dps = events.stream().filter(aue -> {
                            XivCombatant target = aue.getTarget();
                            return (target instanceof XivPlayerCharacter pc) && pc.getJob().isDps();
                        }).findAny();
                        Optional<AbilityUsedEvent> sup = events.stream().filter(aue -> {
                            XivCombatant target = aue.getTarget();
                            return (target instanceof XivPlayerCharacter pc) && !pc.getJob().isDps();
                        }).findAny();

                        if(dps.isPresent()) {
                            XivPlayerCharacter dpsPlayer = (XivPlayerCharacter) dps.get().getTarget();
                            XivPlayerCharacter supPlayer = (XivPlayerCharacter) sup.get().getTarget();
                            s.accept(new SpecificAutoMarkRequest(dpsPlayer, switch (i) {
                                case 1 -> !inverse ? MarkerSign.ATTACK1 : MarkerSign.BIND1;
                                case 2 -> !inverse ? MarkerSign.ATTACK2 : MarkerSign.BIND2;
                                case 3 -> !inverse ? MarkerSign.ATTACK3 : MarkerSign.BIND3;
                                case 4 -> !inverse ? MarkerSign.ATTACK4 : MarkerSign.SQUARE;
                                default -> MarkerSign.IGNORE_NEXT; //Uh oh stinky
                            }));
                            s.accept(new SpecificAutoMarkRequest(supPlayer, switch (i) {
                                case 1 -> inverse ? MarkerSign.ATTACK1 : MarkerSign.BIND1;
                                case 2 -> inverse ? MarkerSign.ATTACK2 : MarkerSign.BIND2;
                                case 3 -> inverse ? MarkerSign.ATTACK3 : MarkerSign.BIND3;
                                case 4 -> inverse ? MarkerSign.ATTACK4 : MarkerSign.SQUARE;
                                default -> MarkerSign.IGNORE_NEXT; //Uh oh stinky
                            }));
                        }
                    }
                }
            });

    @AutoFeed
    private final SequentialTrigger<BaseEvent> highConcept = SqtTemplates.multiInvocation(60_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(31148),
            this::hc1,
            this::hc2);

    // red
    private static final int impAlpha = 0xD02;
    // yellow
    private static final int impBeta = 0xD03;
    // orange
    private static final int impGamma = 0xD04;
    // green on red
    private static final int perfAlpha = 0xD05;
    // yellow on yellow
    private static final int perfBeta = 0xD06;
    // purple on orange
    private static final int perfGamma = 0xD07;
    // 'no' sign
    private static final int inconceivable = 0xD08;
    // red DNA
    private static final int solosplice = 0xD11;
    // yellow DNA
    private static final int multisplice = 0xD12;
    // blue DNA
    private static final int supersplice = 0xD13;

    private void hc1(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {

    }

    private void hc2(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {

    }

    @AutoFeed
    private final SequentialTrigger<BaseEvent> dominion = SqtTemplates.sq(30_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(31193),
            (e1, s) -> {
                List<AbilityUsedEvent> hits = s.waitEventsQuickSuccession(4, AbilityUsedEvent.class, aue -> aue.abilityIdMatches(31195) && aue.isFirstTarget(), Duration.ofMillis(100));
                List<XivPlayerCharacter> hitPlayers = hits.stream()
                        .filter(aue -> (aue.getTarget() instanceof XivPlayerCharacter))
                        .map(aue -> (XivPlayerCharacter)aue.getTarget())
                        .collect(Collectors.toList());
            });

    public BooleanSetting getUseAutoMarks() {
        return useAutoMarks;
    }

    public BooleanSetting getUseLimitlessDesolation() {
        return useLimitlessDesolation;
    }

    public BooleanSetting getUseHC1() {
        return useHC1;
    }

    public BooleanSetting getUseHC2() {
        return useHC2;
    }

    public BooleanSetting getUseDominion() {
        return useLD;
    }

    public BooleanSetting getLDSupportAttack() {
        return LDSupportAttack;
    }
}
