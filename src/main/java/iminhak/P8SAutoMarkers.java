package iminhak;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.MapEffectEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyCommenceEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyRecommenceEvent;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.duties.Pandamonium.P8S2;
import gg.xp.xivsupport.events.triggers.duties.Pandamonium.P8S2DominionPrio;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.ClearAllMarksAction;
import gg.xp.xivsupport.events.triggers.marks.ClearAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.events.triggers.marks.adv.SpecificAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.gui.extra.DutyPluginTab;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@CalloutRepo(name = "P8S Automarkers", duty = KnownDuty.P8S)
public class P8SAutoMarkers extends AutoChildEventHandler implements FilteredEventHandler {

    private static final Logger log = LoggerFactory.getLogger(P8SAutoMarkers.class);

    private final BooleanSetting useAutoMarks;

    private final BooleanSetting useLimitlessDesolation;
    private final BooleanSetting LDSupportAttack;

    private final BooleanSetting useHC1;

    private final BooleanSetting useHC2;

    private final BooleanSetting useLD;

    public P8SAutoMarkers(XivState state, StatusEffectRepository buffs, PersistenceProvider pers, P8S2DominionPrio dominionPrio) {
        this.state = state;
        this.buffs = buffs;
        this.dominionPrio = dominionPrio;
        this.useAutoMarks = new BooleanSetting(pers, "triggers.p8s.use-auto-markers", false);

        this.useLimitlessDesolation = new BooleanSetting(pers, "triggers.p8s.use-dominion", false);
        this.LDSupportAttack = new BooleanSetting(pers, "triggers.p8s.use-LD-sup-attack", false);

        this.useHC1 = new BooleanSetting(pers, "triggers.p8s.use-hc1", false);

        this.useHC2 = new BooleanSetting(pers, "triggers.p8s.use-hc2", false);

        this.useLD = new BooleanSetting(pers, "triggers.p8s.use-ld", false);
    }

    private final XivState state;
    private final StatusEffectRepository buffs;
    private final P8S2DominionPrio dominionPrio;

    private XivState getState() {
        return this.state;
    }

    private StatusEffectRepository getBuffs() {
        return this.buffs;
    }

    private P8S2DominionPrio getDominionPrio() {
        return this.dominionPrio;
    }

    @Override
    public boolean enabled(EventContext context) {
        return state.dutyIs(KnownDuty.P8S);
    }

    @HandleEvents
    public void reset(EventContext context, DutyRecommenceEvent drce) {
        context.accept(new ClearAutoMarkRequest());
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

                s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(31199));
                s.accept(new ClearAutoMarkRequest());
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

    private static boolean towerMapEffect(MapEffectEvent mee) {
        // 257|2022-09-20T20:35:41.6976112-07:00|800375AB|00020001|2E|00|0000|ba9ec316960643c8
        return mee.getFlags() == 0x2_0001;
    }

    private enum TowerColor {
        Blue,
        Purple,
        Green
    }
    /*
		https://discord.com/channels/551474815727304704/594899820976668673/1022314330538127450
		|          | Blue   | Purple | Green |
		| HC1 2x   | 28-29  | 1E-1F  | 32-33 |
		| HC1 4x   | 24-27  | 1A-1D  | 2E-31 |
		| HC2 2x   | 28-29  | 1E-1F  | 32-33 |
		| HC2 NW   | 2A     | 20     | 34    |
		| HC2 NE   | 2B     | 21     | 35    |
		| HC2 SW   | 2C     | 22     | 36    |
		| HC2 SE   | 2D     | 23     | 37    |
	 */
    private static @Nullable P8SAutoMarkers.TowerColor towerColor(List<MapEffectEvent> mapEffects) {
        for (MapEffectEvent mapEffect : mapEffects) {
            log.info("MapEffect: 0x%X".formatted(mapEffect.getIndex()));
            long index = mapEffect.getIndex();
            if (index > 0x24 && index <= 0x2D) {
                return P8SAutoMarkers.TowerColor.Blue;
            }
            else if (index > 0x2E && index <= 0x37) {
                return P8SAutoMarkers.TowerColor.Green;
            }
            else if (index > 0x1A && index <= 0x23) {
                return P8SAutoMarkers.TowerColor.Purple;
            }
        }
        return null;
    }

    private void hc1(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        if(getUseAutoMarks().get() && getUseHC1().get()) {
            List<BuffApplied> buffs = s.waitEvents(8, BuffApplied.class, ba -> ba.buffIdMatches(impAlpha, impBeta, impGamma, supersplice, multisplice));
            //make sure its only players
            buffs = buffs.stream().filter(ba -> {
                XivCombatant target = ba.getTarget();
                return (target instanceof XivPlayerCharacter);
            }).collect(Collectors.toList());
            Optional<XivPlayerCharacter> shortA = buffs.stream().filter(ba -> ba.buffIdMatches(impAlpha) && ba.getInitialDuration().toSeconds() < 15).map(ba -> (XivPlayerCharacter)ba.getTarget()).findAny();
            Optional<XivPlayerCharacter> shortB = buffs.stream().filter(ba -> ba.buffIdMatches(impBeta) && ba.getInitialDuration().toSeconds() < 15).map(ba -> (XivPlayerCharacter)ba.getTarget()).findAny();
            Optional<XivPlayerCharacter> shortG = buffs.stream().filter(ba -> ba.buffIdMatches(impGamma) && ba.getInitialDuration().toSeconds() < 15).map(ba -> (XivPlayerCharacter)ba.getTarget()).findAny();
            Optional<XivPlayerCharacter> longA = buffs.stream().filter(ba -> ba.buffIdMatches(impAlpha) && ba.getInitialDuration().toSeconds() > 15).map(ba -> (XivPlayerCharacter)ba.getTarget()).findAny();
            Optional<XivPlayerCharacter> longB = buffs.stream().filter(ba -> ba.buffIdMatches(impBeta) && ba.getInitialDuration().toSeconds() > 15).map(ba -> (XivPlayerCharacter)ba.getTarget()).findAny();
            Optional<XivPlayerCharacter> longG = buffs.stream().filter(ba -> ba.buffIdMatches(impGamma) && ba.getInitialDuration().toSeconds() > 15).map(ba -> (XivPlayerCharacter)ba.getTarget()).findAny();
            Optional<XivPlayerCharacter> superspliceP = buffs.stream().filter(ba -> ba.buffIdMatches(supersplice)).map(ba -> (XivPlayerCharacter)ba.getTarget()).findAny();
            Optional<XivPlayerCharacter> multispliceP = buffs.stream().filter(ba -> ba.buffIdMatches(multisplice)).map(ba -> (XivPlayerCharacter)ba.getTarget()).findAny();

            //marks first debuffs going off
            s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(inconceivable));
            //markers useless if no mapeffects, so just let it fail if it cant find them
            List<MapEffectEvent> mapEffects = s.waitEvents(2, MapEffectEvent.class, P8SAutoMarkers::towerMapEffect);
            @Nullable TowerColor towerColor = towerColor(mapEffects);
            log.info("HC1AM: Tower Color 1: {}", towerColor);
            String skipped = "Error";
            if(shortA.isPresent() && shortB.isPresent() && shortG.isPresent() && longA.isPresent() && longB.isPresent() && longG.isPresent() && superspliceP.isPresent() && multispliceP.isPresent()) {
                if (towerColor == TowerColor.Green) {
                    s.accept(new SpecificAutoMarkRequest(shortA.get(), MarkerSign.ATTACK1));
                    s.accept(new SpecificAutoMarkRequest(shortB.get(), MarkerSign.ATTACK2));
                    //Gamma skip
                    skipped = "Gamma";
                    s.accept(new SpecificAutoMarkRequest(multispliceP.get(), MarkerSign.BIND1));
                    s.accept(new SpecificAutoMarkRequest(superspliceP.get(), MarkerSign.BIND2));
                } else if(towerColor == TowerColor.Purple) {
                    s.accept(new SpecificAutoMarkRequest(shortB.get(), MarkerSign.ATTACK1));
                    s.accept(new SpecificAutoMarkRequest(shortG.get(), MarkerSign.ATTACK2));
                    //Alpha Skip
                    skipped = "Alpha";
                    s.accept(new SpecificAutoMarkRequest(multispliceP.get(), MarkerSign.BIND2));
                    s.accept(new SpecificAutoMarkRequest(superspliceP.get(), MarkerSign.BIND3));
                } else if(towerColor == TowerColor.Blue) {
                    s.accept(new SpecificAutoMarkRequest(shortA.get(), MarkerSign.ATTACK1));
                    s.accept(new SpecificAutoMarkRequest(shortG.get(), MarkerSign.ATTACK2));
                    //Beta Skip
                    skipped = "Beta";
                    s.accept(new SpecificAutoMarkRequest(multispliceP.get(), MarkerSign.BIND1));
                    s.accept(new SpecificAutoMarkRequest(superspliceP.get(), MarkerSign.BIND3));
                } else {
                    log.info("HC1AM: Unknown tower color: {}", towerColor);
                }

                //second towers appear
                s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(inconceivable));
                s.accept(new ClearAutoMarkRequest());

                TowerColor towerColor2;
                mapEffects = s.waitEvents(2, MapEffectEvent.class, P8SAutoMarkers::towerMapEffect);
                towerColor2 = towerColor(mapEffects);

                if(towerColor2 == TowerColor.Green) {
                    //A and B mix
                    if(skipped.equals("Gamma")) {
                        //Stacks are A and B
                        s.accept(new SpecificAutoMarkRequest(multispliceP.get(), MarkerSign.ATTACK1));
                        s.accept(new SpecificAutoMarkRequest(superspliceP.get(), MarkerSign.ATTACK2));
                    } else if(skipped.equals("Alpha")) {
                        //Stacks are B and G
                        s.accept(new SpecificAutoMarkRequest(shortA.get(), MarkerSign.ATTACK1));
                        s.accept(new SpecificAutoMarkRequest(multispliceP.get(), MarkerSign.ATTACK2));
                    } else if(skipped.equals("Beta")) {
                        //Stacks are A and G
                        s.accept(new SpecificAutoMarkRequest(multispliceP.get(), MarkerSign.ATTACK1));
                        s.accept(new SpecificAutoMarkRequest(shortB.get(), MarkerSign.ATTACK2));
                    } else {
                        log.info("HC1AM: Unknown skipped: {}", skipped);
                    }
                    s.accept(new SpecificAutoMarkRequest(longA.get(), MarkerSign.IGNORE1));
                    s.accept(new SpecificAutoMarkRequest(longB.get(), MarkerSign.IGNORE2));
                } else if(towerColor2 == TowerColor.Purple) {
                    //B and G mix
                    if(skipped.equals("Gamma")) {
                        //Stacks are A and B
                        s.accept(new SpecificAutoMarkRequest(superspliceP.get(), MarkerSign.ATTACK1));
                        s.accept(new SpecificAutoMarkRequest(shortG.get(), MarkerSign.ATTACK2));
                    } else if(skipped.equals("Alpha")) {
                        //Stacks are B and G
                        s.accept(new SpecificAutoMarkRequest(multispliceP.get(), MarkerSign.ATTACK1));
                        s.accept(new SpecificAutoMarkRequest(superspliceP.get(), MarkerSign.ATTACK2));
                    } else if(skipped.equals("Beta")) {
                        //Stacks are A and G
                        s.accept(new SpecificAutoMarkRequest(shortB.get(), MarkerSign.ATTACK1));
                        s.accept(new SpecificAutoMarkRequest(superspliceP.get(), MarkerSign.ATTACK2));
                    } else {
                        log.info("HC1AM: Unknown skipped: {}", skipped);
                    }
                    s.accept(new SpecificAutoMarkRequest(longB.get(), MarkerSign.IGNORE1));
                    s.accept(new SpecificAutoMarkRequest(longG.get(), MarkerSign.IGNORE2));
                } else if(towerColor2 == TowerColor.Blue) {
                    //A and G mix
                    if(skipped.equals("Gamma")) {
                        //Stacks are A and B
                        s.accept(new SpecificAutoMarkRequest(multispliceP.get(), MarkerSign.ATTACK1));
                        s.accept(new SpecificAutoMarkRequest(shortG.get(), MarkerSign.ATTACK2));
                    } else if(skipped.equals("Alpha")) {
                        //Stacks are B and G
                        s.accept(new SpecificAutoMarkRequest(shortA.get(), MarkerSign.ATTACK1));
                        s.accept(new SpecificAutoMarkRequest(superspliceP.get(), MarkerSign.ATTACK2));
                    } else if(skipped.equals("Beta")) {
                        //Stacks are A and G
                        s.accept(new SpecificAutoMarkRequest(multispliceP.get(), MarkerSign.ATTACK1));
                        s.accept(new SpecificAutoMarkRequest(superspliceP.get(), MarkerSign.ATTACK2));
                    } else {
                        log.info("HC1AM: Unknown skipped: {}", skipped);
                    }
                    s.accept(new SpecificAutoMarkRequest(longA.get(), MarkerSign.IGNORE1));
                    s.accept(new SpecificAutoMarkRequest(longG.get(), MarkerSign.IGNORE2));
                } else {
                    log.info("HC1AM: Unknown tower color: {}", towerColor2);
                }
            } else {
                log.info("HC1AM: Something is missing! {}, {}, {}, {}, {}, {}, {}, {}", shortA.isPresent(), shortB.isPresent(), shortG.isPresent(), longA.isPresent(), longB.isPresent(), longG.isPresent(), superspliceP.isPresent(), multispliceP.isPresent());
            }
        }

        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(31199));
        s.accept(new ClearAutoMarkRequest());
    }

    private void hc2(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        if(getUseAutoMarks().get() && getUseHC2().get()) {
            List<BuffApplied> buffs = s.waitEvents(8, BuffApplied.class, ba -> ba.buffIdMatches(impAlpha, impBeta, impGamma, solosplice, multisplice));
            //make sure its only players
            buffs = buffs.stream().filter(ba -> {
                XivCombatant target = ba.getTarget();
                return (target instanceof XivPlayerCharacter);
            }).collect(Collectors.toList());
            Optional<XivPlayerCharacter> shortA = buffs.stream().filter(ba -> ba.buffIdMatches(impAlpha) && ba.getInitialDuration().toSeconds() < 15).map(ba -> (XivPlayerCharacter)ba.getTarget()).findAny();
            Optional<XivPlayerCharacter> shortB = buffs.stream().filter(ba -> ba.buffIdMatches(impBeta) && ba.getInitialDuration().toSeconds() < 15).map(ba -> (XivPlayerCharacter)ba.getTarget()).findAny();
            Optional<XivPlayerCharacter> shortG = buffs.stream().filter(ba -> ba.buffIdMatches(impGamma) && ba.getInitialDuration().toSeconds() < 15).map(ba -> (XivPlayerCharacter)ba.getTarget()).findAny();
            Optional<XivPlayerCharacter> longA = buffs.stream().filter(ba -> ba.buffIdMatches(impAlpha) && ba.getInitialDuration().toSeconds() > 15).map(ba -> (XivPlayerCharacter)ba.getTarget()).findAny();
            Optional<XivPlayerCharacter> longB = buffs.stream().filter(ba -> ba.buffIdMatches(impBeta) && ba.getInitialDuration().toSeconds() > 15).map(ba -> (XivPlayerCharacter)ba.getTarget()).findAny();
            Optional<XivPlayerCharacter> longG = buffs.stream().filter(ba -> ba.buffIdMatches(impGamma) && ba.getInitialDuration().toSeconds() > 15).map(ba -> (XivPlayerCharacter)ba.getTarget()).findAny();
            List<XivPlayerCharacter> ifrits = new ArrayList<>(getState().getPartyList());
            if(shortA.isPresent() && shortB.isPresent() && shortG.isPresent() && longA.isPresent() && longB.isPresent() && longG.isPresent()) {
                buffs.stream().filter(ba -> ba.getTarget() instanceof XivPlayerCharacter).map(ba -> (XivPlayerCharacter) ba.getTarget()).forEach(ifrits::remove);
                if(ifrits.size() == 2) {
                    s.accept(new SpecificAutoMarkRequest(ifrits.get(1), MarkerSign.IGNORE1));
                    s.accept(new SpecificAutoMarkRequest(ifrits.get(2), MarkerSign.IGNORE2));

                    s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(inconceivable));

                    List<MapEffectEvent> mapEffects = s.waitEvents(2, MapEffectEvent.class, P8SAutoMarkers::towerMapEffect);
                    @Nullable TowerColor towerColor = towerColor(mapEffects);
                    log.info("HC2AM: Tower Color 1: {}", towerColor);

                    XivPlayerCharacter skipped = null;
                    if (towerColor == TowerColor.Green) {
                        s.accept(new SpecificAutoMarkRequest(shortA.get(), MarkerSign.ATTACK1));
                        s.accept(new SpecificAutoMarkRequest(shortB.get(), MarkerSign.ATTACK2));
                        //Gamma skip
                        skipped = shortG.get();
                    } else if(towerColor == TowerColor.Purple) {
                        s.accept(new SpecificAutoMarkRequest(shortB.get(), MarkerSign.ATTACK1));
                        s.accept(new SpecificAutoMarkRequest(shortG.get(), MarkerSign.ATTACK2));
                        //Alpha Skip
                        skipped = shortA.get();
                    } else if(towerColor == TowerColor.Blue) {
                        s.accept(new SpecificAutoMarkRequest(shortA.get(), MarkerSign.ATTACK1));
                        s.accept(new SpecificAutoMarkRequest(shortG.get(), MarkerSign.ATTACK2));
                        //Beta Skip
                        skipped = shortB.get();
                    } else {
                        log.info("HC1AM: Unknown tower color: {}", towerColor);
                    }

                    //second towers appear
                    s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(inconceivable));

                    s.accept(new SpecificAutoMarkRequest(longA.get(), MarkerSign.BIND1));
                    s.accept(new SpecificAutoMarkRequest(longB.get(), MarkerSign.BIND2));
                    s.accept(new SpecificAutoMarkRequest(longG.get(), MarkerSign.SQUARE));
                    s.accept(new SpecificAutoMarkRequest(skipped, MarkerSign.TRIANGLE));

                    s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7AA0));
                    s.accept(new ClearAutoMarkRequest());
                }
            } else {
                log.info("HC1AM: Something is missing! {}, {}, {}, {}, {}, {}", shortA.isPresent(), shortB.isPresent(), shortG.isPresent(), longA.isPresent(), longB.isPresent(), longG.isPresent());
            }

        }

    }

    @AutoFeed
    private final SequentialTrigger<BaseEvent> dominion = SqtTemplates.sq(30_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(31193),
            (e1, s) -> {
                List<AbilityUsedEvent> hits = s.waitEventsQuickSuccession(4, AbilityUsedEvent.class, aue -> aue.abilityIdMatches(31195) && aue.isFirstTarget(), Duration.ofMillis(100));
                List<XivPlayerCharacter> firstSet = new ArrayList<>(getState().getPartyList());
                List<XivPlayerCharacter> secondSet = hits.stream()
                        .filter(aue -> (aue.getTarget() instanceof XivPlayerCharacter))
                        .map(aue -> (XivPlayerCharacter)aue.getTarget())
                        .collect(Collectors.toList());
                firstSet.removeAll(secondSet);

                firstSet.sort(getDominionPrio().getSortSetting().getPlayerJailSortComparator());
                secondSet.sort(getDominionPrio().getSortSetting().getPlayerJailSortComparator());

                if(firstSet.size() == 4 && secondSet.size() == 4) {
                    s.accept(new SpecificAutoMarkRequest(firstSet.get(0), MarkerSign.ATTACK1));
                    s.accept(new SpecificAutoMarkRequest(firstSet.get(1), MarkerSign.ATTACK2));
                    s.accept(new SpecificAutoMarkRequest(firstSet.get(2), MarkerSign.ATTACK3));
                    s.accept(new SpecificAutoMarkRequest(firstSet.get(3), MarkerSign.ATTACK4));
                    s.waitMs(100);
                    s.accept(new SpecificAutoMarkRequest(secondSet.get(0), MarkerSign.BIND1));
                    s.accept(new SpecificAutoMarkRequest(secondSet.get(1), MarkerSign.BIND2));
                    s.accept(new SpecificAutoMarkRequest(secondSet.get(2), MarkerSign.BIND3));
                    s.accept(new SpecificAutoMarkRequest(secondSet.get(3), MarkerSign.SQUARE));

                    s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(31196));

                    s.accept(new SpecificAutoMarkRequest(secondSet.get(0), MarkerSign.ATTACK1));
                    s.accept(new SpecificAutoMarkRequest(secondSet.get(1), MarkerSign.ATTACK2));
                    s.accept(new SpecificAutoMarkRequest(secondSet.get(2), MarkerSign.ATTACK3));
                    s.accept(new SpecificAutoMarkRequest(secondSet.get(3), MarkerSign.ATTACK4));

                    s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(31196));
                    s.accept(new ClearAutoMarkRequest());
                } else {
                    log.info("DominionAM: Error in first/second set sizes: {} and {}", firstSet.size(), secondSet.size());
                }
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
