package randoop.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import randoop.test.ExpectedExceptionGenerator;
import randoop.test.ExtendGenerator;
import randoop.test.InvalidCheckGenerator;
import randoop.test.PostConditionCheckGenerator;
import randoop.test.TestCheckGenerator;

/**
 * Records the outcome of checking in prestate all of the {@link BooleanExpression} for the {@link
 * randoop.condition.specification.PreSpecification}, the {@link GuardPropertyPair}, and {@link
 * GuardThrowsPair} for an operation call.
 *
 * <p>Conceptually, represents a table, in which each table entry records:
 *
 * <ol>
 *   <li>Whether the guard expressions for the {@link
 *       randoop.condition.specification.PreSpecification} of the specification fail or are
 *       satisfied. The guard expressions fail if the Boolean expression of any {@link
 *       randoop.condition.specification.PreSpecification} is false. Otherwise, the guard
 *       expressions are satisfied.
 *   <li>The set of {@link ThrowsClause} objects for expected exceptions. An exception is expected
 *       because the operation has a {@link GuardThrowsPair} for which the guard {@link
 *       BooleanExpression} was satisfied.
 *   <li>The expected {@link PropertyExpression}, if any.
 * </ol>
 *
 * <p>For a particular operation call, a table is constructed by calling {@link
 * OperationConditions#checkPrestate(Object[])} and represents the expected outcome(s) from the
 * call. The table is used to create a {@link TestCheckGenerator} by calling {@link
 * #addPostCheckGenerator(TestCheckGenerator)} that, when given to the sequence generator, will
 * classify the call as follows
 *
 * <ol>
 *   <li>For each table entry with a non-empty expected exception set
 *       <ul>
 *         <li>If an exception is thrown by the call and the thrown exception is a member of the
 *             set, then classify as {@link randoop.main.GenInputsAbstract.BehaviorType#EXPECTED}.
 *         <li>If an exception is thrown by the call and the thrown exception is not a member of the
 *             set, classify as {@link randoop.main.GenInputsAbstract.BehaviorType#ERROR} (because
 *             the specification required an exception to be thrown, but it was not thrown).
 *         <li>If no exception is thrown, then classify as {@link
 *             randoop.main.GenInputsAbstract.BehaviorType#ERROR}.
 *       </ul>
 *
 *   <li>If for each table entry, the preconditions failed, classify as {@link
 *       randoop.main.GenInputsAbstract.BehaviorType#INVALID}.
 *   <li>For each table entry where all preconditions were satisfied, check the corresponding normal
 *       post-condition property, if one exists. If any such property fails, then classify as {@link
 *       randoop.main.GenInputsAbstract.BehaviorType#ERROR}.
 * </ol>
 */
public class ExpectedOutcomeTable {

  /** Indicates whether this table is empty. */
  private boolean isEmpty = true;

  /** Indicates whether a pre-expression was satisfied. */
  private boolean hasSatisfiedGuardExpression = false;

  /** The list of guard expressions for which the guard expression was satisfied. */
  private final List<PropertyExpression> postConditions;

  /** The list of sets of throws clauses for which the guard expression was satisfied. */
  private final List<Set<ThrowsClause>> exceptionSets;

  /** Creates an empty {@link ExpectedOutcomeTable}. */
  public ExpectedOutcomeTable() {
    postConditions = new ArrayList<>();
    exceptionSets = new ArrayList<>();
  }

  /**
   * Adds the outcome of checking the pre- and guard expressions of an operation.
   *
   * @param guardIsSatisfied boolean value indicating whether all guard expressions are satisfied
   * @param propertyExpression property expression that must be true in post-state if no exception
   *     is
   * @param throwsClauses set of exception type-comment pairs for exceptions expected in post-state
   */
  void add(
      boolean guardIsSatisfied,
      PropertyExpression propertyExpression,
      Set<ThrowsClause> throwsClauses) {
    // An empty table cannot represent a pre-state for which the call is invalid, so setting isEmpty
    // to false is necessary even if the entry has !guardIsSatisfied and no propertyExpression or
    // throwsClauses
    isEmpty = false;
    if (guardIsSatisfied) {
      if (propertyExpression != null) {
        postConditions.add(propertyExpression);
      }
      hasSatisfiedGuardExpression = true;
    }
    if (!throwsClauses.isEmpty()) {
      exceptionSets.add(throwsClauses);
    }
  }

  /**
   * Indicate whether this set of results indicates a definitively invalid pre-state.
   * (<i>Invalid</i> meaning that the call should be classified as {@link
   * randoop.main.GenInputsAbstract.BehaviorType#INVALID}.)
   *
   * <p>Occurs when all guard expressions fail and there are no expected exceptions.
   *
   * <p>This method should be called after all entries are added; that is, no more entries should be
   * added after it is called.
   *
   * @return true if guard expressions of all specifications are unsatisfied, and there are no
   *     expected exceptions; false, otherwise
   */
  public boolean isInvalidPrestate() {
    return !isEmpty && !hasSatisfiedGuardExpression && exceptionSets.isEmpty();
  }

  /**
   * Constructs the {@link TestCheckGenerator} that will test for expected {@link ThrowsClause}s or
   * {@link PropertyExpression} as follows:
   *
   * <ul>
   *   <li>if this table is empty, returns the given generator.
   *   <li>if this table has expected exceptions, then returns a {@link ExpectedExceptionGenerator}
   *       to check for those exceptions.
   *   <li>if all pre-expressions fail, then return an {@link InvalidCheckGenerator}.
   *   <li>if any {@link GuardPropertyPair} has a satisfied guard expression, then extend the given
   *       generator with a {@link PostConditionCheckGenerator}.
   * </ul>
   *
   * @param gen the generator to extend
   * @return the {@link TestCheckGenerator} to check for expected outcomes in this table
   */
  public TestCheckGenerator addPostCheckGenerator(TestCheckGenerator gen) {
    if (isEmpty) {
      return gen;
    }

    // if there are expected exceptions, then override guard expressions
    if (!exceptionSets.isEmpty()) {
      return new ExpectedExceptionGenerator(exceptionSets);
    }

    // had conflict with throws guard expressions
    if (!hasSatisfiedGuardExpression) {
      gen = new InvalidCheckGenerator();
    }

    if (!postConditions.isEmpty()) {
      return new ExtendGenerator(new PostConditionCheckGenerator(postConditions), gen);
    }

    return gen;
  }
}
